package com.ampznetwork.testbot;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.text.AttributedCharacterIterator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.imageio.ImageIO;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

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
                                    : new URL("https://github.com/burdoto/TestingBot/raw/master/src/main/resources/Background.png").openStream();
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

    public static InputStream component2img(Component component) {
        int width  = backgroundImage.getWidth();  // Use the background image width
        int height = backgroundImage.getHeight(); // Use the background image height

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

        // Apply text styling
        applyStyledText(g2d, component, width, height);

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

    private static void applyStyledText(Graphics2D g2d, Component component, int width, int height) {
        // Extract text and styles from the Kyori Adventure Component
        String text = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);

        // Text layout for better control over positioning and styling
        FontRenderContext frc    = g2d.getFontRenderContext();
        TextLayout        layout = new TextLayout(text, g2d.getFont(), frc);

        // Center text horizontally and vertically
        int textWidth  = (int) layout.getBounds().getWidth();
        int textHeight = (int) layout.getBounds().getHeight();
        int x          = (width - textWidth) / 2;
        int y          = (int) ((float) (height - textHeight) / 2 + layout.getAscent());

        // Apply styles
        Style     style     = component.style();
        TextColor textColor = style.color();

        // Convert textColor to Color with opaque alpha
        Color awtColor = textColor != null ? new Color(textColor.value()) : Color.WHITE;
        g2d.setColor(awtColor);

        // Apply bold and italic
        Map<AttributedCharacterIterator.Attribute, Object> attributes = new HashMap<>();
        if (style.hasDecoration(TextDecoration.BOLD)) {
            attributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
        }
        if (style.hasDecoration(TextDecoration.ITALIC)) {
            attributes.put(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
        }
        if (style.hasDecoration(TextDecoration.UNDERLINED)) {
            attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        }

        // Draw the styled text
        g2d.setFont(g2d.getFont().deriveFont(attributes));
        g2d.drawString(text, x, y);
    }
}
