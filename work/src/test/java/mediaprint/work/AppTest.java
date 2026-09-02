package mediaprint.work;

import java.nio.file.Files;
import java.nio.file.Path;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfVersion;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import mediaprint.imbustatrice.Imbustatrice;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        assertTrue( true );
    }

    public void testDeleteTemporaryFile() throws Exception
    {
        Path temporaryFile = Files.createTempFile("workpdf-cleanup-", ".pdf");
        assertTrue(Files.exists(temporaryFile));

        assertTrue(App.deleteTemporaryFile(temporaryFile.toString(), message -> fail(message)));
        assertFalse(Files.exists(temporaryFile));
    }

    public void testPdfProcessingReleasesInputAndOutputFiles() throws Exception
    {
        Path input = Files.createTempFile("workpdf-input-", ".pdf");
        Path output = Files.createTempFile("workpdf-output-", ".pdf");
        try {
            try (PdfDocument document = new PdfDocument(new PdfWriter(input.toString()))) {
                document.addNewPage();
            }

            Imbustatrice.copyFirstPage(input.toString(), output.toString(), PdfVersion.PDF_1_7);

            assertTrue(App.deleteTemporaryFile(input.toString(), message -> fail(message)));
            assertTrue(App.deleteTemporaryFile(output.toString(), message -> fail(message)));
            assertFalse(Files.exists(input));
            assertFalse(Files.exists(output));
        } finally {
            Files.deleteIfExists(input);
            Files.deleteIfExists(output);
        }
    }

    public void testPdfConstructionFailureStillReleasesFiles() throws Exception
    {
        Path invalidInput = Files.createTempFile("workpdf-invalid-", ".pdf");
        Path output = Files.createTempFile("workpdf-failed-output-", ".pdf");
        Files.writeString(invalidInput, "not a PDF");
        boolean failedAsExpected = false;
        try {
            Imbustatrice.copyFirstPage(invalidInput.toString(), output.toString(), PdfVersion.PDF_1_7);
        } catch (Exception expected) {
            failedAsExpected = true;
        }

        assertTrue(failedAsExpected);
        assertTrue(App.deleteTemporaryFile(invalidInput.toString(), message -> fail(message)));
        assertTrue(App.deleteTemporaryFile(output.toString(), message -> fail(message)));
        assertFalse(Files.exists(invalidInput));
        assertFalse(Files.exists(output));
    }
}
