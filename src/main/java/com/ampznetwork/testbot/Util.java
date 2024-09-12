package com.ampznetwork.testbot;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.AttributedString;
import java.util.Objects;
import java.util.Optional;

import static java.awt.font.TextAttribute.*;

public class Util {
    private static final Font          font;
    private static final BufferedImage backgroundImage;

    static {
        try (
                var fontResource = Util.class.getResourceAsStream("Minecraft.ttf");
                var fontFallback = fontResource != null
                                   ? null
                                   : new URL("https://github.com/burdoto/TestingBot/raw/master/src/main/resources/Minecraft.ttf").openStream();
                var imageResource = Util.class.getResourceAsStream("Background.png");
                var imageFallback = imageResource != null
                                    ? null
                                    : new URL("https://github.com/burdoto/TestingBot/raw/master/src/main/resources/Background.png").openStream()
        ) {
            // Load font
            font = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNullElse(fontResource, fontFallback));
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(font);

            // Load background image
            backgroundImage = ImageIO.read(Objects.requireNonNullElse(imageResource, imageFallback));
            if (backgroundImage == null) {
                throw new RuntimeException("Unable to load background image.");
            }
        } catch (IOException | FontFormatException e) {
            throw new RuntimeException("Unable to load resources", e);
        }
    }

    public static InputStream component2img(TextComponent component) {
        int width  = backgroundImage.getWidth();
        int height = backgroundImage.getHeight() / 4;

        // Create a new BufferedImage to draw on
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D    g2d           = bufferedImage.createGraphics();

        // Draw the background image
        g2d.drawImage(backgroundImage, 0, 0, null);

        // Set rendering hints for anti-aliasing and better quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Set the font to be twice as large
        Font originalFont = font.deriveFont(Font.PLAIN, 48);  // Increase font size as needed
        g2d.setFont(originalFont);

        // Create an AttributedString based on the Kyori Adventure Component
        AttributedString attributedString = buildAttributedStringFromComponent(component);
        attributedString.addAttribute(FONT, originalFont);

        // Calculate text dimensions
        FontMetrics metrics    = g2d.getFontMetrics(originalFont);
        var         iterator   = attributedString.getIterator();
        int         textWidth  = iterator.getEndIndex() - iterator.getBeginIndex();
        int         textHeight = metrics.getHeight();

        // Center text horizontally and vertically
        int x = 50;//(width - textWidth) / 3;
        int y = (height - textHeight) / 2 + metrics.getAscent();

        // Draw the styled text
        g2d.drawString(attributedString.getIterator(), x, y);

        // Dispose of the Graphics2D object
        g2d.dispose();

        // Create an InputStream from the BufferedImage
        InputStream inputStream = null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Write the BufferedImage to the ByteArrayOutputStream
            ImageIO.write(bufferedImage, "png", baos);
            baos.flush();

            // Convert ByteArrayOutputStream to ByteArrayInputStream
            inputStream = new ByteArrayInputStream(baos.toByteArray());

            System.out.println("PNG image is ready as InputStream.");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return inputStream;
    }

    private static AttributedString buildAttributedStringFromComponent(TextComponent component) {
        var plainText        = PlainTextComponentSerializer.plainText().serialize(component);
        var attributedString = new AttributedString(plainText);
        appendStyle(attributedString, component, new int[]{ 0 });
        return attributedString;
    }

    private static void appendStyle(AttributedString str, TextComponent component, int[] offset) {
        int len   = component.content().length(), end = offset[0] + len;
        var style = component.style();

        if (len > 0) {
            var color = Optional.ofNullable(style.color())
                    .map(textColor -> new Color(textColor.value()))
                    .orElse(Color.WHITE);
            str.addAttribute(FOREGROUND, color, offset[0], end);

            for (var decor : TextDecoration.values())
                if (decor != TextDecoration.OBFUSCATED && style.hasDecoration(decor))
                    str.addAttribute(
                            switch (decor) {
                                case BOLD -> WEIGHT;
                                case STRIKETHROUGH -> STRIKETHROUGH;
                                case UNDERLINED -> UNDERLINE;
                                case ITALIC -> POSTURE;
                                default -> throw new IllegalStateException("Unexpected value: " + decor);
                            },
                            switch (decor) {
                                case BOLD -> WEIGHT_BOLD;
                                case STRIKETHROUGH -> STRIKETHROUGH_ON;
                                case UNDERLINED -> UNDERLINE_ON;
                                case ITALIC -> POSTURE_OBLIQUE;
                                default -> throw new IllegalStateException("Unexpected value: " + decor);
                            },
                            offset[0], end);
        }

        offset[0] += len;
        for (var child : component.children())
            if (child instanceof TextComponent txt)
                appendStyle(str, txt, offset);
    }
}
