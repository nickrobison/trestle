package com.nickrobison.trestle.raster.common;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

/**
 * Created by nrobison on 9/26/16.
 */
public class RasterConverter {

    public enum ImageType {
        PNG("png"),
        JPEG("jpeg"),
        TIFF("tiff");

        private final String type;

        private ImageType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return this.type;
        }
    }


    /**
     * Convert a RenderedImage to the destination output format
     *
     * @param image - RenderedImage source image
     * @param type  - ImageType of output file
     * @param file  - Output file to write to
     * @throws IOException
     */
    public static void convertImage(RenderedImage image, ImageType type, File file) throws IOException {
        ImageIO.write(image, type.toString(), file);
    }
}
