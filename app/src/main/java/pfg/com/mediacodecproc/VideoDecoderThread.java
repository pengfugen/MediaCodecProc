package pfg.com.mediacodecproc;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by FPENG3 on 2018/5/17.
 */

public class VideoDecoderThread extends Thread implements DecoderInterface{

    private String TAG = "VideoDecoderThread";

    private MediaExtractor mExtrator;
    private MediaCodec mCodec;
    private Surface mSurface;
    private boolean eosReceived;

    @Override
    public boolean init(SurfaceHolder holder, String filePath) {
        eosReceived = false;
        mExtrator = new MediaExtractor();
        try {
            mExtrator.setDataSource(filePath);
            int trackcount = mExtrator.getTrackCount();
            Util.logi(TAG, "trackcount: "+ trackcount);
            for(int i = 0; i < trackcount; i++) {
                MediaFormat format = mExtrator.getTrackFormat(i); // 分离出音轨和视轨

                String mime = format.getString(MediaFormat.KEY_MIME);
                int width =  format.getInteger(MediaFormat.KEY_WIDTH);
                int height = format.getInteger(MediaFormat.KEY_HEIGHT);


                /*if(width > 0 && height > 0) {
                    holder.setFixedSize(width, height);
                }*/
                if(mime.startsWith("video/")) {
                    mExtrator.selectTrack(i);
                    Util.logi(TAG, "before MediaFormat: "+ format);
                    mCodec = MediaCodec.createDecoderByType(mime);

                    /*int []colorFormats= mCodec.getCodecInfo().getCapabilitiesForType(mime).colorFormats;
                    for(int color : colorFormats) {
                        Util.logi(TAG, "colorFormats: "+ color);
                    }*/
                    mSurface = holder.getSurface();
                    format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
                    mCodec.configure(format, holder.getSurface(), null, 0);
                    mCodec.start();
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void run() {
        BufferInfo info = new BufferInfo();
        boolean isInput = true;
        boolean first = false;
        long startWhen = 0;
        int frameCount = 0;
        while(!eosReceived) {
            if(isInput) {
                int inputIndex = mCodec.dequeueInputBuffer(1000);
                Util.logi(TAG, "inputindex: "+ inputIndex);
                if(inputIndex >= 0) {
                    ByteBuffer inputBuffer =  mCodec.getInputBuffer(inputIndex);
                    int sampleSize = mExtrator.readSampleData(inputBuffer, 0);

                    if (sampleSize > 0) {
                        mCodec.queueInputBuffer(inputIndex, 0, sampleSize, mExtrator.getSampleTime(), 0);
                        mExtrator.advance();

                    } else {
                        Util.logi(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                        mCodec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        isInput = false;
                    }
                }
            }


            int outputIndex = mCodec.dequeueOutputBuffer(info, 1000);
            Util.logi(TAG, "outputIndex: "+ outputIndex);

            /*switch (outputIndex) {
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    Util.logi(TAG, "INFO_OUTPUT_FORMAT_CHANGED format : " + mCodec.getOutputFormat());
                    break;

                case MediaCodec.INFO_TRY_AGAIN_LATER:
//                Log.d(TAG, "INFO_TRY_AGAIN_LATER");
                    break;

                default:
                    if (!first) {
                        startWhen = System.currentTimeMillis();
                        first = true;
                    }
                    try {
                        long sleepTime = (info.presentationTimeUs / 1000) - (System.currentTimeMillis() - startWhen);
                        //Log.d(TAG, "info.presentationTimeUs : " + (info.presentationTimeUs / 1000) + " playTime: " + (System.currentTimeMillis() - startWhen) + " sleepTime : " + sleepTime);

                        if (sleepTime > 0)
                            Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    Image image = mCodec.getOutputImage(outputIndex);
                    int width = image.getWidth();
                    int height = image.getHeight();
                    int format = image.getFormat();
                    Util.logi(TAG, " Image info: width=" + width + " height="+height+" format="+format);

                    mCodec.releaseOutputBuffer(outputIndex, true *//* Surface init *//*);
                    break;
            }*/
            if((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                break;
            }

            if(outputIndex >= 0 && info.size != 0) {
                MediaFormat afterFormat = mCodec.getOutputFormat(outputIndex);
                Util.logi(TAG, "after MediaFormat: "+ afterFormat);
                Image image = mCodec.getOutputImage(outputIndex);
                if(image != null) {
                    int format = image.getFormat();

                    Util.logi(TAG, " Image info: format="+format);
                    if(format == ImageFormat.YUV_420_888) {
                        int width = image.getWidth();
                        int height = image.getHeight();
                        Image.Plane []planes = image.getPlanes();
                        Util.logi(TAG, "=========================");
                        Util.logi(TAG, " Image info: format="+format+" image:"+image);
                        for(int i = 0; i < planes.length; i++) {
                            Util.logi(TAG, " width:"+width+" height:"+height+" planes["+i+"]:"+" PixelStride:"+planes[i].getPixelStride()+" RowStride:"+planes[i].getRowStride()+" buffersize:"+planes[i].getBuffer().remaining());
                            Util.logi(TAG, "Image BitsPerPixel:"+ImageFormat.getBitsPerPixel(format));
                        }
                        Util.logi(TAG, "=========================");
                        frameCount++;
                        if (frameCount <= 10) {
                            /*byte[] data = new byte[width * height];
                            planes[0].getBuffer().get(data, 0, data.length);
                            Util.logi(TAG, "Plane[0] start, remaining:"+planes[0].getBuffer().remaining());
                            StringBuffer buffer = new StringBuffer();
                            for (int i = 0; i < data.length; i++) {
                                buffer.append(data[i]).append(" ");
                                if((i%30) == 0 && i != 0) {
                                    buffer.append("\n");
                                }
                            }
                            Util.logi(TAG, buffer.toString());


                            Util.logi(TAG, "Plane[1] start, remaining:"+planes[1].getBuffer().remaining());
                            StringBuffer buffer1 = new StringBuffer();
                            for (int i = 0; i < planes[1].getBuffer().remaining(); i++) {
                                buffer1.append(planes[1].getBuffer().get(i)).append(" ");
                                if((i%30) == 0 && i != 0) {
                                    buffer1.append("\n");
                                }
                            }
                            Util.logi(TAG, buffer1.toString());

                            Util.logi(TAG, "Plane[2] start, remaining:"+planes[2].getBuffer().remaining());
                            StringBuffer buffer2 = new StringBuffer();
                            for (int i = 0; i < planes[2].getBuffer().remaining(); i++) {
                                buffer2.append(planes[2].getBuffer().get(i)).append(" ");
                                if((i%30) == 0 && i != 0) {
                                    buffer2.append("\n");
                                }
                            }
                            Util.logi(TAG, buffer2.toString());*/

                            String filePath = "/sdcard/test/" + "test_" + frameCount + ".jpeg";
                            compressToJpeg(filePath, image);
                        }

                    }
                }

                try {
                    long sleepTime = (info.presentationTimeUs / 1000) - (System.currentTimeMillis() - startWhen);
                    //Log.d(TAG, "info.presentationTimeUs : " + (info.presentationTimeUs / 1000) + " playTime: " + (System.currentTimeMillis() - startWhen) + " sleepTime : " + sleepTime);

                    if (sleepTime > 0)
                        Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                mCodec.releaseOutputBuffer(outputIndex, true /* Surface init */);
            }

        }

        mCodec.stop();
        mCodec.release();
        mExtrator.release();

    }

    private byte[] getRawCopy(ByteBuffer in) {
        ByteBuffer rawCopy = ByteBuffer.allocate(in.capacity());
        rawCopy.put(in);
        return rawCopy.array();
    }

    private void fastReverse(byte[] array, int offset, int length) {
        int end = offset + length;
        for (int i = offset; i < offset + (length / 2); i++) {
            array[i] = (byte) (array[i] ^ array[end - i - 1]);
            array[end - i - 1] = (byte) (array[i] ^ array[end - i - 1]);
            array[i] = (byte) (array[i] ^ array[end - i - 1]);
        }
    }

    /*private ByteBuffer convertYUV420ToN21(Image imgYUV420, boolean grayscale) {
        Image.Plane yPlane = imgYUV420.getPlanes()[0];
        byte[] yData = getRawCopy(yPlane.getBuffer());
        Image.Plane uPlane = imgYUV420.getPlanes()[1];
        byte[] uData = getRawCopy(uPlane.getBuffer());
        Image.Plane vPlane = imgYUV420.getPlanes()[2];
        byte[] vData = getRawCopy(vPlane.getBuffer()); // NV21 stores a full frame luma (y) and half frame chroma (u,v), so total size is // size(y) + size(y) / 2 + size(y) / 2 = size(y) + size(y) / 2 * 2 = size(y) + size(y) = 2 * size(y)
        int npix = imgYUV420.getWidth() * imgYUV420.getHeight();
        byte[] nv21Image = new byte[npix * 2];
        Arrays.fill(nv21Image, (byte) 127); // 127 -> 0 chroma (luma will be overwritten in either case)
        // Copy the Y-plane
        ByteBuffer nv21Buffer = ByteBuffer.wrap(nv21Image);
        for (int i = 0; i < imgYUV420.getHeight(); i++) {
            nv21Buffer.put(yData, i * yPlane.getRowStride(), imgYUV420.getWidth());
        } // Copy the u and v planes interlaced
        if (!grayscale) {
            for (int row = 0; row < imgYUV420.getHeight() / 2; row++) {
                for (int cnt = 0, upix = 0, vpix = 0; cnt < imgYUV420.getWidth() / 2; upix += uPlane.getPixelStride(), vpix += vPlane.getPixelStride(), cnt++) {
                    nv21Buffer.put(uData[row * uPlane.getRowStride() + upix]);
                    nv21Buffer.put(vData[row * vPlane.getRowStride() + vpix]);
                }
            }
            fastReverse(nv21Image, npix, npix);
        }
        fastReverse(nv21Image, 0, npix);
        return nv21Buffer;
    }

    private byte[] convertYUV420ToN21(Image imgYUV420) {
        byte[] rez = new byte[0];
        ByteBuffer buffer0 = imgYUV420.getPlanes()[0].getBuffer();
        ByteBuffer buffer2 = imgYUV420.getPlanes()[2].getBuffer();
        int buffer0_size = buffer0.remaining();
        int buffer2_size = buffer2.remaining();
        rez = new byte[buffer0_size + buffer2_size];
        buffer0.get(rez, 0, buffer0_size);
        buffer2.get(rez, buffer0_size, buffer2_size);
        return rez;
    }*/

    private void compressToJpeg(String fileName, Image image) {
        FileOutputStream outStream;
        try {
            outStream = new FileOutputStream(fileName);
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to create output file " + fileName, ioe);
        }
        Rect rect = image.getCropRect();
        YuvImage yuvImage = new YuvImage(mygetDataFromImage(image, ImageFormat.NV21), ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        yuvImage.compressToJpeg(rect, 100, outStream);
    }

    private boolean isSupportedImageFormat(int format) {
        if(ImageFormat.NV21 == format || ImageFormat.YUY2 == format)
            return true;
        else
            return false;
    }

    private byte[] othersgetDataFromImage(Image image, int convertFormat) {
        if(!isSupportedImageFormat(convertFormat)) {
            throw new RuntimeException("It is not supported format!");
        }

        int imageFormat = image.getFormat();
        if(imageFormat != ImageFormat.YUV_420_888) {
            throw new RuntimeException("It is not YUV_420_888!");
        }

        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Util.logi(TAG, "Image info width:"+image.getWidth()+" height:"+image.getHeight()+" Rect info width:"+width+" height:"+height);
        Util.logi(TAG, "Image BitsPerPixel:"+ImageFormat.getBitsPerPixel(format));

        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
        Util.logi(TAG, "BitsPerPixel: "+(ImageFormat.getBitsPerPixel(format) / 8));
        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    if (convertFormat == ImageFormat.YUV_420_888) {
                        channelOffset = width * height;
                        outputStride = 1;
                    } else if (convertFormat == ImageFormat.NV21) {
                        channelOffset = width * height;
                        outputStride = 2;
                    }
                    break;
                case 2:
                    if (convertFormat == ImageFormat.YUV_420_888) {
                        channelOffset = (int) (width * height * 1.25);
                        outputStride = 1;
                    } else if (convertFormat == ImageFormat.NV21) {
                        channelOffset = width * height + 1;
                        outputStride = 2;
                    }
                    break;
            }
            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();
            /*if (VERBOSE) {
                Log.v(TAG, "pixelStride " + pixelStride);
                Log.v(TAG, "rowStride " + rowStride);
                Log.v(TAG, "width " + width);
                Log.v(TAG, "height " + height);
                Log.v(TAG, "buffer size " + buffer.remaining());
            }*/
            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
            Util.logi(TAG, "Finished reading data from plane " + i);
        }
        return data;
    }

    // YUV_420_888 converted to NV21
    private byte[] mygetDataFromImage(Image image, int convertFormat) {
        if(!isSupportedImageFormat(convertFormat)) {
            throw new RuntimeException("It is not supported format!");
        }

        int imageFormat = image.getFormat();
        if(imageFormat != ImageFormat.YUV_420_888) {
            throw new RuntimeException("It is not YUV_420_888!");
        }

        int width = image.getWidth();
        int height = image.getHeight();
        Rect rect = image.getCropRect();
        Util.logi(TAG, "Image info width:"+width+" height:"+height+" Rect info width:"+width+" height:"+height);

        Image.Plane []planes = image.getPlanes();
        Util.logi(TAG, "Image info Y's remaining:"+planes[0].getBuffer().remaining()+" U's remaining:"+planes[1].getBuffer().remaining());

        byte []data = new byte[width*height+((width*height)/2)]; //YUV_420_888表示一个像素占一个byte，假如不是一个像素占一个byte就需要另外考虑

        // 存储结构为YYYYYYYYYYYYYYYYVUVUVUVU
        for(int i = 0; i < planes.length; i++) {
            switch(i) {
                case 0:
                    planes[i].getBuffer().get(data, 0, planes[i].getBuffer().remaining());
                    break;
                case 1:
                    byte []tempU = new byte[planes[i].getBuffer().remaining()];
                    planes[i].getBuffer().get(tempU, 0, planes[i].getBuffer().remaining());
                    int tempUIndex = 0;
                    for(int u = width*height+1; u < data.length; u +=2) {
                        data[u] = tempU[tempUIndex];
                        tempUIndex++;
                    }
                    break;
                case 2:
                    byte []tempV = new byte[planes[i].getBuffer().remaining()];
                    planes[i].getBuffer().get(tempV, 0, planes[i].getBuffer().remaining());
                    int tempVIndex = 0;
                    for(int v = width*height; v < data.length; v +=2) { //NV21是V在U前面
                        data[v] = tempV[tempVIndex];
                        tempVIndex++;
                    }
                    break;
            }
        }

        return data;
    }

    @Override
    public void finalize() {
        eosReceived = true;
    }
}
