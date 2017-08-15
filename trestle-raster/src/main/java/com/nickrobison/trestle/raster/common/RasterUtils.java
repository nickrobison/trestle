package com.nickrobison.trestle.raster.common;

import sun.awt.image.ByteInterleavedRaster;

import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferShort;
import java.awt.image.Raster;
import java.nio.ByteBuffer;

/**
 * Created by nrobison on 9/28/16.
 */
public class RasterUtils {

    /**
     * Converts a Raster tile to a byte array
     *
     * @param tile - Raster to extract data from
     * @return - byte array of Raster tile
     */
    public static final byte[] extractRasterData(Raster tile) {
        final int numBands = tile.getNumBands();
        final int dataType = tile.getDataBuffer().getDataType();
        final int bankSize = tile.getDataBuffer().getSize();
        byte[] bank = new byte[bankSize];
        ByteBuffer outputBuffer;
        if (tile instanceof ByteInterleavedRaster) {
//            If the bytes are interleaved, we can just read one bank and move on
            outputBuffer = ByteBuffer.allocate(bankSize);
            final DataBufferByte dataBuffer = (DataBufferByte) tile.getDataBuffer();
            outputBuffer.put(dataBuffer.getData());
            return outputBuffer.array();
        } else {
            outputBuffer = ByteBuffer.allocate(dataType * bankSize);
        }

        for (int i = 0; i < numBands; i++) {
            if (dataType == Float.BYTES) {
                final float[] data = ((DataBufferFloat) tile.getDataBuffer()).getData(i);
                ByteBuffer buffer = ByteBuffer.allocate(Float.BYTES * data.length);

                for (float value : data) {
                    buffer.putFloat(value);
                }

                bank = buffer.array();
            } else if (dataType == Short.BYTES) {
                final short[] data = ((DataBufferShort) tile.getDataBuffer()).getData(i);
                ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES * data.length);

                for (short value : data) {
                    buffer.putShort(value);
                }
                bank = buffer.array();
            } else {
//                If we can just pull out the bytes themselves, we just need to make sure we aren't reading the non-existent band info
                final DataBufferByte dataBuffer = (DataBufferByte) tile.getDataBuffer();
                if (i < dataBuffer.getNumBanks()) {
                    bank = dataBuffer.getData(i);
                } else {
                    continue;
                }
            }
            outputBuffer.put(bank);
        }

        return outputBuffer.array();
    }
}
