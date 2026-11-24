import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

/// Draws the application icon: a white globe on a blue rounded square, with an amber "Aa"
/// badge from 32 pixels on.
///
/// Run from the project directory with
/// `java tools/IconGenerator.java src/main/resources/name/ulbricht/locale/explorer/ui/icons src/packaging`.
/// Writes `app-<size>.png` for the window icons and, for jpackage, a Windows icon
/// (`windows/locale-explorer.ico`), a macOS icon (`macos/locale-explorer.icns`) and a Linux
/// icon (`linux/locale-explorer.png`).
public final class IconGenerator {

    private static final int[] SIZES = {16, 24, 32, 48, 64, 128, 256};
    private static final Color BLUE = new Color(0x2F6FBF);
    private static final Color AMBER = new Color(0xF2A93B);
    private static final Color DARK = new Color(0x3D2600);

    /// The badge font. Without it Java would silently use another font and the icon would
    /// change, so generating the icons needs a system that has it, such as Windows.
    private static final Font BADGE_FONT = requireFont("Segoe UI");

    private IconGenerator() {
    }

    /// Writes the icons.
    ///
    /// @param args the PNG directory for the window icons and the packaging directory
    /// @throws IOException if a file cannot be written
    public static void main(String[] args) throws IOException {
        var pngDirectory = Path.of(args[0]);
        var packaging = Path.of(args[1]);
        Files.createDirectories(pngDirectory);
        var pngs = new ArrayList<byte[]>();
        for (var size : SIZES) {
            var png = png(draw(size));
            Files.write(pngDirectory.resolve("app-" + size + ".png"), png);
            pngs.add(png);
        }
        write(packaging.resolve("windows/locale-explorer.ico"), ico(pngs));
        write(packaging.resolve("macos/locale-explorer.icns"), icns());
        write(packaging.resolve("linux/locale-explorer.png"), png(draw(512)));
    }

    private static void write(Path file, byte[] content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, content);
    }

    private static Font requireFont(String family) {
        var font = new Font(family, Font.BOLD, 1);
        if (!font.getFamily(java.util.Locale.ROOT).equals(family)) {
            throw new IllegalStateException("Font " + family + " is not installed; the icons would look different");
        }
        return font;
    }

    static BufferedImage draw(int size) {
        var image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        var inset = size * 0.03;
        var corner = size * 0.24;
        g.setColor(BLUE);
        g.fill(new RoundRectangle2D.Double(inset, inset, size - 2 * inset, size - 2 * inset, corner, corner));

        var withBadge = size >= 32;
        var globe = withBadge ? size * 0.60 : size * 0.70;
        var x = withBadge ? size * 0.12 : (size - globe) / 2;
        var y = withBadge ? size * 0.12 : (size - globe) / 2;
        drawGlobe(g, x, y, globe, Math.max(1.0, size * 0.055), size >= 32);

        if (withBadge) {
            drawBadge(g, size);
        }
        g.dispose();
        return image;
    }

    private static void drawGlobe(Graphics2D g, double x, double y, double diameter, double stroke, boolean detailed) {
        var outline = new Ellipse2D.Double(x, y, diameter, diameter);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke((float) stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(outline);
        g.draw(new Ellipse2D.Double(x + diameter * 0.30, y, diameter * 0.40, diameter));
        g.draw(new Line2D.Double(x, y + diameter / 2, x + diameter, y + diameter / 2));
        if (detailed) {
            var clip = g.getClip();
            g.clip(outline);
            g.draw(new Line2D.Double(x, y + diameter * 0.25, x + diameter, y + diameter * 0.25));
            g.draw(new Line2D.Double(x, y + diameter * 0.75, x + diameter, y + diameter * 0.75));
            g.setClip(clip);
        }
    }

    private static void drawBadge(Graphics2D g, int size) {
        var width = size * 0.50;
        var height = size * 0.36;
        var left = size - width - size * 0.07;
        var top = size - height - size * 0.07;
        g.setColor(AMBER);
        g.fill(new RoundRectangle2D.Double(left, top, width, height, height * 0.5, height * 0.5));
        g.setColor(BLUE);
        g.setStroke(new BasicStroke((float) Math.max(1.0, size * 0.025)));
        g.draw(new RoundRectangle2D.Double(left, top, width, height, height * 0.5, height * 0.5));

        g.setColor(DARK);
        var font = BADGE_FONT.deriveFont((float) (height * 0.78));
        g.setFont(font);
        var metrics = g.getFontMetrics();
        var text = "Aa";
        var textX = left + (width - metrics.stringWidth(text)) / 2;
        var textY = top + (height - metrics.getHeight()) / 2 + metrics.getAscent();
        g.drawString(text, (float) textX, (float) textY);
    }

    private static byte[] png(BufferedImage image) throws IOException {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return bytes.toByteArray();
    }

    /// Builds a macOS ICNS file whose entries are PNG images: 128, 256, 512 and 1024 pixels.
    private static byte[] icns() throws IOException {
        var types = new String[] {"ic07", "ic08", "ic09", "ic10"};
        var sizes = new int[] {128, 256, 512, 1024};
        var entries = new ByteArrayOutputStream();
        for (var i = 0; i < types.length; i++) {
            var png = png(draw(sizes[i]));
            entries.write(types[i].getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            entries.write(ByteBuffer.allocate(4).putInt(8 + png.length).array());
            entries.write(png);
        }
        var file = new ByteArrayOutputStream();
        file.write("icns".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        file.write(ByteBuffer.allocate(4).putInt(8 + entries.size()).array());
        entries.writeTo(file);
        return file.toByteArray();
    }

    /// Builds a Windows ICO file whose entries are PNG images (supported since Windows Vista).
    private static byte[] ico(List<byte[]> pngs) {
        var headerSize = 6 + 16 * pngs.size();
        var total = headerSize + pngs.stream().mapToInt(png -> png.length).sum();
        var buffer = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short) 0).putShort((short) 1).putShort((short) pngs.size());
        var offset = headerSize;
        for (var i = 0; i < pngs.size(); i++) {
            var size = SIZES[i];
            buffer.put((byte) (size >= 256 ? 0 : size)).put((byte) (size >= 256 ? 0 : size));
            buffer.put((byte) 0).put((byte) 0);
            buffer.putShort((short) 1).putShort((short) 32);
            buffer.putInt(pngs.get(i).length).putInt(offset);
            offset += pngs.get(i).length;
        }
        pngs.forEach(buffer::put);
        return buffer.array();
    }
}
