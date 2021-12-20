package edu.nd.crc.paperanalyticaldevices;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.metadata.MetadataExtractor;
import org.tensorflow.lite.support.metadata.schema.ModelMetadata;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.stream.Collectors;

public class TensorflowNetwork {
    private final Interpreter mInterpretor;
    private TensorImage TensorImage;
    private final ImageProcessor mImageProcessor;
    private final TensorBuffer TensorProbability;
    public final List<String> Labels;

    private TensorflowNetwork(Interpreter interpreter, List<String> labels) {
        mInterpretor = interpreter;
        Labels = labels;

        // Setup Inputs
        Tensor input = mInterpretor.getInputTensor(0);

        TensorImage = new TensorImage(input.dataType());
        mImageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(input.shape()[2], input.shape()[1], ResizeOp.ResizeMethod.BILINEAR))
                .build();

        // Setup Outputs
        Tensor output = mInterpretor.getOutputTensor(0);
        TensorProbability = TensorBuffer.createFixedSize(output.shape(), output.dataType());
    }

    public static class Result {
        public String Label;
        public float Probability;

        public Result(String label, float probability) {
            Label = label;
            Probability = probability;
        }

        @Override
        public String toString() {
            return String.format("%s (%.3f)", Label, Probability);
        }
    }

    public Result infer(final Bitmap image) {
        TensorImage.load(image);
        TensorImage = mImageProcessor.process(TensorImage);

        mInterpretor.run(TensorImage.getBuffer(), TensorProbability.getBuffer());
        float[] probArray = TensorProbability.getFloatArray();
        int maxidx = findMaxIndex(probArray);

        return new Result(Labels.get(maxidx), TensorProbability.getFloatArray()[maxidx]);
    }

    private static int findMaxIndex(float[] arr) {
        float max = arr[0];
        int maxIdx = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > max) {
                max = arr[i];
                maxIdx = i;
            }
        }
        return maxIdx;
    }

    public static TensorflowNetwork from(Context context, String filename) throws IOException {
        File tensorFile = new File(context.getDir("tflitemodels", Context.MODE_PRIVATE).getPath(), filename);

        MappedByteBuffer tfliteModel;
        try (FileInputStream input = new FileInputStream(tensorFile)) {
            tfliteModel = input.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, tensorFile.length());
        }

        // does it have metadata?
        MetadataExtractor metadata = new MetadataExtractor(tfliteModel);
        if (!metadata.hasMetadata()) {
            throw new IOException("metadata missing from model");
        }

        // get labels
        List<String> Labels;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(metadata.getAssociatedFile("labels.txt")))) {
            Labels = r.lines().collect(Collectors.toList());
        }

        // other metadata
        ModelMetadata mm = metadata.getModelMetadata();
        Log.e("GBR", mm.description());
        Log.e("GBR", mm.version());

        return new TensorflowNetwork(new Interpreter(tfliteModel, new Interpreter.Options()), Labels);
    }
}
