package com.ampznetwork.testbot;

import net.kyori.adventure.text.Component;
import org.comroid.api.Polyfill;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.AttributedString;
import java.util.Objects;

public final class Util {
    private Util() {}

    private static final Font font;

    static {
        try (
                var resource = Util.class.getResourceAsStream("Minecraft.ttf");
                var fallback = resource != null ? null : Polyfill.url("").openStream()
        ) {
            font = Font.createFont(0, Objects.requireNonNullElse(resource, fallback));
        } catch (IOException | FontFormatException e) {
            throw new RuntimeException("Unable to load font", e);
        }
    }

    public static InputStream component2img(Component component) {
        int width  = 400;
        int height = 200;

        // Create a BufferedImage object
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Get the Graphics2D object
        Graphics2D g2d = bufferedImage.createGraphics();

        // Set rendering hints for anti-aliasing and better quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Set the background color (optional, if you want a background color)
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // Set the font
        g2d.setFont(font);

        // Create an AttributedString to hold text with styles
        String           text             = "Hello, Colorful World!";
        AttributedString attributedString = new AttributedString(text);

        // Apply color
        attributedString.addAttribute(TextAttribute.FOREGROUND, Color.RED, 0, 5); // "Hello"
        attributedString.addAttribute(TextAttribute.FOREGROUND, Color.BLUE, 7, 15); // "Colorful"

        // Apply bold and italic style
        attributedString.addAttribute(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD, 7, 15); // "Colorful"
        attributedString.addAttribute(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE, 16, 22); // "World"

        // Apply underline
        attributedString.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON, 16, 22); // "World"

        // Draw the styled text
        g2d.drawString(attributedString.getIterator(), 50, 100);

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

            // Now you can use `inputStream` as needed
            System.out.println("PNG image is ready as InputStream.");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // `inputStream` now contains the image data as a PNG image
        // You can use it wherever an InputStream is needed
        return inputStream;
    }
}
