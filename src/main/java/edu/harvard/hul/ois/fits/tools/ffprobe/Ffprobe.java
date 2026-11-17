package edu.harvard.hul.ois.fits.tools.ffprobe;

import edu.harvard.hul.ois.fits.Fits;
import edu.harvard.hul.ois.fits.exceptions.FitsToolException;
import edu.harvard.hul.ois.fits.tools.ToolBase;
import edu.harvard.hul.ois.fits.tools.ToolOutput;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;

/** Extracts MP4 stream metadata without supplying a format identity. */
public class Ffprobe extends ToolBase {

    private static final String XSLT = Fits.FITS_XML_DIR + "ffprobe/ffprobe_to_fits.xslt";
    private final Fits fits;
    private boolean enabled = true;

    public Ffprobe(Fits fits) throws FitsToolException {
        this.fits = fits;
        info.setName("ffprobe");
        info.setVersion("unknown");
        getSaxBuilder().setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    }

    @Override
    public Boolean canIdentify() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public ToolOutput extractInfo(File file) throws FitsToolException {
        long start = System.currentTimeMillis();
        Process probe = null;
        Path probeOutput = null;
        try {
            probeOutput = Files.createTempFile("fits-ffprobe-", ".xml");
            probe = new ProcessBuilder(
                            "ffprobe",
                            "-v",
                            "error",
                            "-protocol_whitelist",
                            "file",
                            "-format_whitelist",
                            "mov",
                            "-show_entries",
                            "program_version=version:format=bit_rate:"
                                    + "stream=index,codec_name,codec_type,codec_tag_string,channels:"
                                    + "stream_disposition=attached_pic",
                            "-of",
                            "xml",
                            "-i",
                            file.getAbsolutePath())
                    .redirectOutput(probeOutput.toFile())
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!probe.waitFor(60, TimeUnit.SECONDS)) {
                throw new FitsToolException("ffprobe timed out processing " + file.getName());
            }
            if (probe.exitValue() != 0) {
                throw new FitsToolException("ffprobe exited with status " + probe.exitValue()
                        + " processing " + file.getName());
            }

            Document rawOutput = getSaxBuilder().build(probeOutput.toFile());
            Element version = rawOutput.getRootElement().getChild("program_version");
            if (version != null) {
                info.setVersion(version.getAttributeValue("version", "unknown"));
            }
            Document fitsXml = transform(XSLT, rawOutput);
            ToolOutput result = new ToolOutput(this, fitsXml, rawOutput, fits);
            duration = System.currentTimeMillis() - start;
            runStatus = RunStatus.SUCCESSFUL;
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FitsToolException("Interrupted while running ffprobe", e);
        } catch (IOException | JDOMException e) {
            throw new FitsToolException("Could not run ffprobe or read its XML output", e);
        } finally {
            if (probe != null && probe.isAlive()) {
                probe.destroyForcibly();
            }
            if (probeOutput != null) {
                try {
                    Files.deleteIfExists(probeOutput);
                } catch (IOException e) {
                    probeOutput.toFile().deleteOnExit();
                }
            }
        }
    }
}
