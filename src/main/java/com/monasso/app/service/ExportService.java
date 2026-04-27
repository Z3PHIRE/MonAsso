package com.monasso.app.service;

import com.monasso.app.config.BrandingConfig;
import com.monasso.app.model.Contribution;
import com.monasso.app.model.Event;
import com.monasso.app.model.Member;
import com.monasso.app.repository.ContributionRepository;
import com.monasso.app.repository.EventRepository;
import com.monasso.app.repository.MemberRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExportService {

    private static final DateTimeFormatter FILE_SUFFIX = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final float PDF_MARGIN = 42f;
    private static final float PDF_TOP_OFFSET = 48f;
    private static final float PDF_LINE_HEIGHT = 13f;
    private static final int PDF_MAX_CHARS_PER_LINE = 150;

    private final MemberRepository memberRepository;
    private final EventRepository eventRepository;
    private final ContributionRepository contributionRepository;
    private final BrandingService brandingService;

    public ExportService(
            MemberRepository memberRepository,
            EventRepository eventRepository,
            ContributionRepository contributionRepository,
            BrandingService brandingService
    ) {
        this.memberRepository = memberRepository;
        this.eventRepository = eventRepository;
        this.contributionRepository = contributionRepository;
        this.brandingService = brandingService;
    }

    public Path exportMembers(Path directory) {
        return exportMembersCsv(directory);
    }

    public Path exportEvents(Path directory) {
        return exportEventsCsv(directory);
    }

    public Path exportContributions(Path directory) {
        return exportContributionsCsv(directory);
    }

    public Path exportMembersCsv(Path directory) {
        List<Member> members = loadMembers();
        List<String> headers = memberHeaders();
        List<List<String>> rows = new ArrayList<>();
        for (Member member : members) {
            rows.add(memberRow(member));
        }
        return writeCsv(directory, buildFilename("members", ".csv"), headers, rows);
    }

    public Path exportEventsCsv(Path directory) {
        List<Event> events = loadEvents();
        List<String> headers = List.of("ID", "Titre", "Date", "Heure", "Lieu", "Description", "Capacite", "Participants");
        List<List<String>> rows = new ArrayList<>();
        for (Event event : events) {
            rows.add(List.of(
                    String.valueOf(event.id()),
                    defaultValue(event.title()),
                    event.eventDate().toString(),
                    event.eventTime().toString(),
                    defaultValue(event.location()),
                    defaultValue(event.description()),
                    event.capacity() == null ? "" : String.valueOf(event.capacity()),
                    String.valueOf(event.participantCount())
            ));
        }
        return writeCsv(directory, buildFilename("events", ".csv"), headers, rows);
    }

    public Path exportContributionsCsv(Path directory) {
        List<Contribution> contributions = loadContributions();
        List<String> headers = List.of("ID", "ID membre", "Membre", "Montant", "Date", "Periode", "Statut", "Paiement", "Notes");
        List<List<String>> rows = new ArrayList<>();
        for (Contribution contribution : contributions) {
            rows.add(List.of(
                    String.valueOf(contribution.id()),
                    String.valueOf(contribution.memberId()),
                    defaultValue(contribution.memberName()),
                    String.format(Locale.US, "%.2f", contribution.amount()),
                    contribution.contributionDate().toString(),
                    defaultValue(contribution.periodLabel()),
                    contribution.status().name(),
                    defaultValue(contribution.paymentMethod()),
                    defaultValue(contribution.notes())
            ));
        }
        return writeCsv(directory, buildFilename("contributions", ".csv"), headers, rows);
    }

    public Path exportMembersXlsx(Path directory) {
        List<Member> members = loadMembers();
        return writeXlsx(directory, buildFilename("members", ".xlsx"), workbook -> {
            List<String> headers = memberHeaders();
            List<List<String>> rows = new ArrayList<>();
            for (Member member : members) {
                rows.add(memberRow(member));
            }
            createSheet(workbook, "Membres", headers, rows);
        });
    }

    public Path exportEventsXlsx(Path directory) {
        List<Event> events = loadEvents();
        return writeXlsx(directory, buildFilename("events", ".xlsx"), workbook -> {
            List<String> headers = List.of("ID", "Titre", "Date", "Heure", "Lieu", "Description", "Capacite", "Participants");
            List<List<String>> rows = new ArrayList<>();
            for (Event event : events) {
                rows.add(List.of(
                        String.valueOf(event.id()),
                        defaultValue(event.title()),
                        event.eventDate().toString(),
                        event.eventTime().toString(),
                        defaultValue(event.location()),
                        defaultValue(event.description()),
                        event.capacity() == null ? "" : String.valueOf(event.capacity()),
                        String.valueOf(event.participantCount())
                ));
            }
            createSheet(workbook, "Evenements", headers, rows);
        });
    }

    public Path exportContributionsXlsx(Path directory) {
        List<Contribution> contributions = loadContributions();
        return writeXlsx(directory, buildFilename("contributions", ".xlsx"), workbook -> {
            List<String> headers = List.of("ID", "ID membre", "Membre", "Montant", "Date", "Periode", "Statut", "Paiement", "Notes");
            List<List<String>> rows = new ArrayList<>();
            for (Contribution contribution : contributions) {
                rows.add(List.of(
                        String.valueOf(contribution.id()),
                        String.valueOf(contribution.memberId()),
                        defaultValue(contribution.memberName()),
                        String.format(Locale.US, "%.2f", contribution.amount()),
                        contribution.contributionDate().toString(),
                        defaultValue(contribution.periodLabel()),
                        contribution.status().name(),
                        defaultValue(contribution.paymentMethod()),
                        defaultValue(contribution.notes())
                ));
            }
            createSheet(workbook, "Cotisations", headers, rows);
        });
    }

    public Path exportGlobalXlsx(Path directory) {
        List<Member> members = loadMembers();
        List<Event> events = loadEvents();
        List<Contribution> contributions = loadContributions();

        return writeXlsx(directory, buildFilename("global_export", ".xlsx"), workbook -> {
            List<List<String>> memberRows = new ArrayList<>();
            for (Member member : members) {
                memberRows.add(memberRow(member));
            }
            createSheet(
                    workbook,
                    "Membres",
                    memberHeaders(),
                    memberRows
            );

            List<List<String>> eventRows = new ArrayList<>();
            for (Event event : events) {
                eventRows.add(List.of(
                        String.valueOf(event.id()),
                        defaultValue(event.title()),
                        event.eventDate().toString(),
                        event.eventTime().toString(),
                        defaultValue(event.location()),
                        defaultValue(event.description()),
                        event.capacity() == null ? "" : String.valueOf(event.capacity()),
                        String.valueOf(event.participantCount())
                ));
            }
            createSheet(
                    workbook,
                    "Evenements",
                    List.of("ID", "Titre", "Date", "Heure", "Lieu", "Description", "Capacite", "Participants"),
                    eventRows
            );

            List<List<String>> contributionRows = new ArrayList<>();
            for (Contribution contribution : contributions) {
                contributionRows.add(List.of(
                        String.valueOf(contribution.id()),
                        String.valueOf(contribution.memberId()),
                        defaultValue(contribution.memberName()),
                        String.format(Locale.US, "%.2f", contribution.amount()),
                        contribution.contributionDate().toString(),
                        defaultValue(contribution.periodLabel()),
                        contribution.status().name(),
                        defaultValue(contribution.paymentMethod()),
                        defaultValue(contribution.notes())
                ));
            }
            createSheet(
                    workbook,
                    "Cotisations",
                    List.of("ID", "ID membre", "Membre", "Montant", "Date", "Periode", "Statut", "Paiement", "Notes"),
                    contributionRows
            );
        });
    }

    public Path exportMembersPdf(Path directory) {
        List<Member> members = loadMembers();
        List<String> lines = new ArrayList<>();
        lines.add("Total membres: " + members.size());
        lines.add("");
        for (Member member : members) {
            lines.add(String.format(
                    Locale.FRANCE,
                    "#%d | %s | type: %s | email: %s | tel: %s | adhesion: %s | statut: %s | role: %s | adresse: %s | notes: %s",
                    member.id(),
                    member.fullName(),
                    member.personTypeLabel(),
                    defaultValue(member.email()),
                    defaultValue(member.phone()),
                    member.joinDate(),
                    member.statusLabel(),
                    defaultValue(member.associationRole()),
                    defaultValue(member.address()),
                    defaultValue(member.notes())
            ));
        }
        return writePdf(directory, buildFilename("members_report", ".pdf"), "Rapport membres", lines);
    }

    public Path exportEventsPdf(Path directory) {
        List<Event> events = loadEvents();
        List<String> lines = new ArrayList<>();
        lines.add("Total evenements: " + events.size());
        lines.add("");
        for (Event event : events) {
            lines.add(String.format(
                    Locale.FRANCE,
                    "#%d | %s | %s %s | lieu: %s | capacite: %s | participants: %d | description: %s",
                    event.id(),
                    defaultValue(event.title()),
                    event.eventDate(),
                    event.eventTime(),
                    defaultValue(event.location()),
                    event.capacity() == null ? "libre" : event.capacity(),
                    event.participantCount(),
                    defaultValue(event.description())
            ));
        }
        return writePdf(directory, buildFilename("events_report", ".pdf"), "Rapport evenements", lines);
    }

    public Path exportContributionsPdf(Path directory) {
        List<Contribution> contributions = loadContributions();
        double total = contributions.stream().mapToDouble(Contribution::amount).sum();
        List<String> lines = new ArrayList<>();
        lines.add(String.format(Locale.FRANCE, "Total cotisations: %d | Montant cumule: %.2f EUR", contributions.size(), total));
        lines.add("");
        for (Contribution contribution : contributions) {
            lines.add(String.format(
                    Locale.FRANCE,
                    "#%d | membre: %s (#%d) | %.2f EUR | date: %s | periode: %s | statut: %s | paiement: %s | notes: %s",
                    contribution.id(),
                    defaultValue(contribution.memberName()),
                    contribution.memberId(),
                    contribution.amount(),
                    contribution.contributionDate(),
                    defaultValue(contribution.periodLabel()),
                    contribution.status().name(),
                    defaultValue(contribution.paymentMethod()),
                    defaultValue(contribution.notes())
            ));
        }
        return writePdf(directory, buildFilename("contributions_report", ".pdf"), "Rapport cotisations", lines);
    }

    private Path writeCsv(Path directory, String fileName, List<String> headers, List<List<String>> rows) {
        try {
            Path exportDirectory = requireDirectory(directory, "CSV");
            Files.createDirectories(exportDirectory);
            Path target = exportDirectory.resolve(fileName);
            List<String> lines = new ArrayList<>();
            lines.add(csvLine(headers));
            for (List<String> row : rows) {
                lines.add(csvLine(row));
            }
            Files.write(target, lines, StandardCharsets.UTF_8);
            return target;
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'exporter le fichier CSV " + fileName, e);
        }
    }

    private Path writeXlsx(Path directory, String fileName, WorkbookConsumer workbookConsumer) {
        try {
            Path exportDirectory = requireDirectory(directory, "XLSX");
            Files.createDirectories(exportDirectory);
            Path target = exportDirectory.resolve(fileName);
            try (Workbook workbook = new XSSFWorkbook(); OutputStream outputStream = Files.newOutputStream(target)) {
                workbookConsumer.accept(workbook);
                workbook.write(outputStream);
            }
            return target;
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'exporter le fichier XLSX " + fileName, e);
        }
    }

    private Path writePdf(Path directory, String fileName, String reportTitle, List<String> lines) {
        try {
            Path exportDirectory = requireDirectory(directory, "PDF");
            Files.createDirectories(exportDirectory);
            Path target = exportDirectory.resolve(fileName);
            BrandingConfig brandingConfig = brandingService.getCurrentBranding();
            Path logoPath = brandingService.getResolvedLogoPath();
            String appName = brandingConfig.appName() == null || brandingConfig.appName().isBlank()
                    ? "MonAsso"
                    : brandingConfig.appName().trim();

            try (PDDocument document = new PDDocument()) {
                try (PdfTextWriter writer = new PdfTextWriter(document, appName, logoPath, reportTitle)) {
                    for (String line : lines) {
                        if (line == null || line.isBlank()) {
                            writer.writeBlankLine();
                        } else {
                            writer.writeLine(line, false);
                        }
                    }
                }
                document.save(target.toFile());
            }
            return target;
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'exporter le fichier PDF " + fileName, e);
        }
    }

    private void createSheet(Workbook workbook, String sheetName, List<String> headers, List<List<String>> rows) {
        Sheet sheet = workbook.createSheet(sheetName);

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setVerticalAlignment(VerticalAlignment.TOP);
        dataStyle.setWrapText(true);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(defaultValue(headers.get(i)));
            cell.setCellStyle(headerStyle);
        }

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            Row row = sheet.createRow(rowIndex + 1);
            List<String> values = rows.get(rowIndex);
            for (int colIndex = 0; colIndex < headers.size(); colIndex++) {
                String value = colIndex < values.size() ? defaultValue(values.get(colIndex)) : "";
                Cell cell = row.createCell(colIndex);
                cell.setCellValue(value);
                cell.setCellStyle(dataStyle);
            }
        }

        for (int colIndex = 0; colIndex < headers.size(); colIndex++) {
            sheet.autoSizeColumn(colIndex);
            int width = sheet.getColumnWidth(colIndex);
            sheet.setColumnWidth(colIndex, Math.min(width + 1024, 20000));
        }
        sheet.createFreezePane(0, 1);
    }

    private List<String> memberHeaders() {
        return List.of(
                "ID",
                "Prenom",
                "Nom",
                "Type",
                "Email",
                "Telephone",
                "Statut",
                "Date entree",
                "Role",
                "Adresse",
                "Competences",
                "Disponibilites",
                "Contact urgence",
                "Taille vetements",
                "Certifications",
                "Contraintes",
                "Documents",
                "Notes"
        );
    }

    private List<String> memberRow(Member member) {
        return List.of(
                String.valueOf(member.id()),
                defaultValue(member.firstName()),
                defaultValue(member.lastName()),
                member.personTypeLabel(),
                defaultValue(member.email()),
                defaultValue(member.phone()),
                member.statusLabel(),
                member.joinDate().toString(),
                defaultValue(member.associationRole()),
                defaultValue(member.address()),
                defaultValue(member.skills()),
                defaultValue(member.availability()),
                defaultValue(member.emergencyContact()),
                defaultValue(member.clothingSize()),
                defaultValue(member.certifications()),
                defaultValue(member.constraintsInfo()),
                defaultValue(member.linkedDocuments()),
                defaultValue(member.notes())
        );
    }

    private List<Member> loadMembers() {
        return memberRepository.findByCriteria("", null);
    }

    private List<Event> loadEvents() {
        return eventRepository.findByCriteria("", false);
    }

    private List<Contribution> loadContributions() {
        return contributionRepository.findByCriteria("", null, null);
    }

    private String csvLine(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(';');
            }
            builder.append(csvCell(values.get(i)));
        }
        return builder.toString();
    }

    private String csvCell(String value) {
        String safe = defaultValue(value);
        String escaped = safe.replace("\"", "\"\"");
        if (escaped.contains(";") || escaped.contains("\n") || escaped.contains("\r") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private String buildFilename(String prefix, String extension) {
        return prefix + "_" + FILE_SUFFIX.format(LocalDateTime.now()) + extension;
    }

    private String defaultValue(String value) {
        return value == null ? "" : value;
    }

    private Path requireDirectory(Path directory, String exportType) {
        if (directory == null) {
            throw new IllegalArgumentException("Le dossier de destination est obligatoire pour l'export " + exportType + ".");
        }
        return directory.toAbsolutePath().normalize();
    }

    @FunctionalInterface
    private interface WorkbookConsumer {
        void accept(Workbook workbook) throws IOException;
    }

    private static final class PdfTextWriter implements AutoCloseable {

        private final PDDocument document;
        private final String appName;
        private final Path logoPath;
        private final String reportTitle;

        private PDPageContentStream contentStream;
        private float cursorY;

        private PdfTextWriter(PDDocument document, String appName, Path logoPath, String reportTitle) throws IOException {
            this.document = document;
            this.appName = appName;
            this.logoPath = logoPath;
            this.reportTitle = reportTitle;
            startPage(false);
        }

        private void writeLine(String text, boolean bold) throws IOException {
            ensureSpace();
            contentStream.beginText();
            contentStream.setFont(bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, 10);
            contentStream.newLineAtOffset(PDF_MARGIN, cursorY);
            contentStream.showText(pdfSafe(text));
            contentStream.endText();
            cursorY -= PDF_LINE_HEIGHT;
        }

        private void writeBlankLine() throws IOException {
            ensureSpace();
            cursorY -= PDF_LINE_HEIGHT * 0.6f;
        }

        private void ensureSpace() throws IOException {
            if (cursorY < PDF_MARGIN + PDF_LINE_HEIGHT) {
                startPage(true);
            }
        }

        private void startPage(boolean continuation) throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);

            float top = page.getMediaBox().getHeight() - PDF_MARGIN;
            float textStartX = PDF_MARGIN;
            boolean logoDrawn = false;

            if (logoPath != null && Files.exists(logoPath)) {
                try {
                    PDImageXObject image = PDImageXObject.createFromFileByContent(logoPath.toFile(), document);
                    float logoHeight = 28f;
                    float logoWidth = image.getWidth() * (logoHeight / image.getHeight());
                    contentStream.drawImage(image, PDF_MARGIN, top - logoHeight + 4, logoWidth, logoHeight);
                    textStartX += logoWidth + 10;
                    logoDrawn = true;
                } catch (IOException ignored) {
                    logoDrawn = false;
                }
            }

            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
            contentStream.newLineAtOffset(textStartX, top);
            contentStream.showText(pdfSafe(appName));
            contentStream.endText();

            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
            contentStream.newLineAtOffset(textStartX, top - 16);
            contentStream.showText(pdfSafe(continuation ? reportTitle + " (suite)" : reportTitle));
            contentStream.endText();

            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 9);
            contentStream.newLineAtOffset(textStartX, top - 29);
            contentStream.showText("Genere le " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            contentStream.endText();

            float lineY = logoDrawn ? top - 34 : top - 36;
            contentStream.setLineWidth(0.8f);
            contentStream.moveTo(PDF_MARGIN, lineY);
            contentStream.lineTo(page.getMediaBox().getWidth() - PDF_MARGIN, lineY);
            contentStream.stroke();

            cursorY = lineY - PDF_TOP_OFFSET + 12;
        }

        private String pdfSafe(String text) {
            String safe = text == null ? "" : text;
            safe = safe.replace("\r", " ").replace("\n", " ").trim();
            safe = Normalizer.normalize(safe, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < safe.length(); i++) {
                char c = safe.charAt(i);
                if (c >= 32 && c <= 126) {
                    builder.append(c);
                } else {
                    builder.append('?');
                }
                if (builder.length() >= PDF_MAX_CHARS_PER_LINE) {
                    break;
                }
            }
            return builder.toString();
        }

        @Override
        public void close() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }
        }
    }
}
