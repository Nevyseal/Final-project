package app.ij.mlwithtensorflowlite;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import app.ij.mlwithtensorflowlite.ml.Partwisemodel;

public class MainActivity extends AppCompatActivity {

    private static final int INPUT_SIZE = 128;
    private static final int GALLERY_REQ = 1, CAMERA_REQ = 3;
    private static final String TAG = "MainActivity";

    private ImageView carPartImageView;
    private TextView partNameTextView, partDescriptionTextView;
    private LinearProgressIndicator difficultyBar;
    private FrameLayout loadingOverlay;
    private MaterialButton cameraBtn, galleryBtn, buyBtnAmazon, buyBtnEbay;
    private BottomNavigationView bottomNavigationView;
    private MaterialCardView partDetailsCard;  // Ensure this is declared
    // Define the car part details
    private final Object[][] partDetails = {
            {
                    "Air Intake",
                    "Toyota",
                    "2005",
                    "Nairobi Auto Spares",
                    "Front",
                    5,
                    "The air intake system on this 2005 Toyota model is a crucial component responsible for channeling clean, filtered air into the engine for optimal combustion. Proper airflow improves engine efficiency, power output, and fuel economy. Over time, the air intake can accumulate dust and debris, which may reduce performance, making timely replacement or cleaning essential. Sourced from Nairobi Auto Spares, this genuine part ensures compatibility and durability for your vehicle.",
                    "https://www.amazon.com/s?k=Toyota+2005+Air+Intake",
                    "https://www.ebay.com/sch/i.html?_nkw=Toyota+2005+Air+Intake"
            },
            {
                    "Fog Light",
                    "Prado 75 Series",
                    "2000",
                    "AutoXpress",
                    "Front",
                    4,
                    "Designed specifically for the Prado 75 Series from the year 2000, this fog light enhances visibility during adverse weather conditions such as fog, heavy rain, or snow. Positioned low on the front bumper, it emits a wide, low beam to illuminate the road surface without reflecting off water droplets, reducing glare. The replacement difficulty is moderate, and sourcing it from AutoXpress guarantees a quality fit and finish that meets original equipment standards.",
                    "https://www.amazon.com/s?k=Prado+75+Series+2000+Fog+Light",
                    "https://www.ebay.com/sch/i.html?_nkw=Prado+75+Series+2000+Fog+Light"
            },
            {
                    "Headlight",
                    "TOYOTA",
                    "2018",
                    "eBay",
                    "Front",
                    6,
                    "This headlight assembly for the 2018 toyota combines advanced optics and durable materials to provide bright, focused illumination for nighttime driving. Featuring halogen or LED options, it ensures safety by improving road visibility and signaling your presence to other drivers. The installation requires moderate technical skill due to electrical connections and alignment adjustments. Available on eBay, this aftermarket part offers a cost-effective solution without compromising quality.",
                    "https://www.amazon.com/s?k=Nissan+NV200+2018+Headlight",
                    "https://www.ebay.com/sch/i.html?_nkw=Nissan+NV200+2018+Headlight"
            },
            {
                    "Tail Light",
                    "Peugeot 406",
                    "2002",
                    "eBay",
                    "Rear",
                    3,
                    "The tail light for the 2002 Peugeot 406 is an essential safety feature that signals braking and vehicle presence to drivers behind you. This rear-positioned light incorporates brake lights, turn signals, and sometimes reverse lights, all housed in a weather-resistant casing. Replacement is relatively straightforward, making it a common DIY task. Sourced from eBay, this part balances affordability with reliable performance, ensuring your vehicle remains road-legal and safe.",
                    "https://www.amazon.com/s?k=Peugeot+406+2002+Tail+Light",
                    "https://www.ebay.com/sch/i.html?_nkw=Peugeot+406+2002+Tail+Light"
            }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        setupListeners();



    }

    @SuppressLint("WrongViewCast")
    private void bindViews() {
        carPartImageView = findViewById(R.id.carPartImageView);
        cameraBtn = findViewById(R.id.buttonCamera);
        galleryBtn = findViewById(R.id.buttonGallery);
        buyBtnAmazon = findViewById(R.id.buyOnAmazonButton);
        buyBtnEbay = findViewById(R.id.buyOnEbayButton);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        partNameTextView = findViewById(R.id.partNameTextView);
        partDescriptionTextView = findViewById(R.id.partDescriptionTextView);
        partDetailsCard = findViewById(R.id.partDetailsCard);  // Initialize partDetailsCard

    }

    private void setupListeners() {
        cameraBtn.setOnClickListener(v -> handleCameraIntent());
        galleryBtn.setOnClickListener(v -> handleGalleryIntent());
    }



    private void handleCameraIntent() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), CAMERA_REQ);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
        }
    }

    private void handleGalleryIntent() {
        startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI), GALLERY_REQ);
    }

    @Override
    protected void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null) return;

        Bitmap bmp = null;
        try {
            bmp = handleImageResult(req, data);
        } catch (IOException e) {
            Log.e(TAG, "Error processing image", e);
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
        }

        if (bmp != null) classifyImage(bmp);
    }

    private Bitmap handleImageResult(int req, Intent data) throws IOException {
        Bitmap bmp = null;
        if (req == CAMERA_REQ && data.getExtras() != null) {
            bmp = (Bitmap) data.getExtras().get("data");
        } else if (req == GALLERY_REQ && data.getData() != null) {
            Uri uri = data.getData();
            bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        }
        return bmp;
    }

    private void classifyImage(Bitmap image) {
        loadingOverlay.setVisibility(View.VISIBLE);

        new Thread(() -> {
            Bitmap scaled = preprocessImage(image);

            try {
                Partwisemodel model = Partwisemodel.newInstance(this);
                try {
                    float[] outputs = runModel(scaled, model);
                    int bestMatch = findBestMatch(outputs);

                    runOnUiThread(() -> displayResults(scaled, bestMatch));

                } finally {
                    model.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error running TensorFlow Lite model", e);
                runOnUiThread(() -> {
                    loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private Bitmap preprocessImage(Bitmap image) {
        Bitmap square = centerCrop(image);
        return Bitmap.createScaledBitmap(square, INPUT_SIZE, INPUT_SIZE, true);
    }

    private float[] runModel(Bitmap scaled, Partwisemodel model) {
        ByteBuffer buf1 = toByteBuffer(scaled);
        ByteBuffer buf2 = toByteBuffer(horizontalFlip(scaled));

        TensorBuffer in1 = TensorBuffer.createFixedSize(new int[]{1, INPUT_SIZE, INPUT_SIZE, 3}, DataType.FLOAT32);
        TensorBuffer in2 = TensorBuffer.createFixedSize(new int[]{1, INPUT_SIZE, INPUT_SIZE, 3}, DataType.FLOAT32);
        in1.loadBuffer(buf1);
        in2.loadBuffer(buf2);

        float[] out1 = model.process(in1).getOutputFeature0AsTensorBuffer().getFloatArray();
        float[] out2 = model.process(in2).getOutputFeature0AsTensorBuffer().getFloatArray();

        return averageOutputs(out1, out2);
    }

    private float[] averageOutputs(float[] out1, float[] out2) {
        float[] avg = new float[out1.length];
        for (int i = 0; i < avg.length; i++) {
            avg[i] = (out1[i] + out2[i]) / 2f;
        }
        return avg;
    }

    private int findBestMatch(float[] outputs) {
        int best = 0;
        for (int i = 1; i < outputs.length; i++) {
            if (outputs[i] > outputs[best]) {
                best = i;
            }
        }
        return best;
    }

    private void displayResults(Bitmap scaled, int bestMatch) {
        Object[] det = partDetails[bestMatch];
        String name = (String) det[0];
        String description = (String) det[6];
        String amazonLink = (String) det[7];
        String ebayLink = (String) det[8];
        try {
            float confPct = calculateConfidence(runModel(scaled,Partwisemodel.newInstance(this)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        carPartImageView.setImageBitmap(scaled);
        partNameTextView.setText(name);
        partDescriptionTextView.setText(description);
        partDetailsCard.setVisibility(View.VISIBLE);

        Log.d(TAG, "Part Name: " + name);
        Log.d(TAG, "Description: " + description);
        Log.d(TAG, "Amazon Link: " + amazonLink);
        Log.d(TAG, "Ebay Link: " + ebayLink);

        buyBtnAmazon.setOnClickListener(v -> openUri(amazonLink));
        buyBtnEbay.setOnClickListener(v -> openUri(ebayLink));

        loadingOverlay.setVisibility(View.GONE);
    }

    private void openUri(String uriString) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
        try {
            startActivity(browserIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening URI", e);
            Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show();
        }
    }
    private float calculateConfidence(float[] outputs) {
        float maxOutput = 0;
        for (float output : outputs) {
            maxOutput = Math.max(maxOutput, output);
        }
        return maxOutput * 100f; // Convert to percentage
    }

    private Bitmap centerCrop(Bitmap bm) {
        int w = bm.getWidth(), h = bm.getHeight(), s = Math.min(w, h);
        return Bitmap.createBitmap(bm, (w - s) / 2, (h - s) / 2, s, s);
    }

    private Bitmap horizontalFlip(Bitmap bm) {
        Matrix m = new Matrix();
        m.preScale(-1, 1);
        return Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
    }


    private ByteBuffer toByteBuffer(Bitmap bm) {
        ByteBuffer buf = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3);
        buf.order(ByteOrder.nativeOrder());
        int[] px = new int[INPUT_SIZE * INPUT_SIZE];
        bm.getPixels(px, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);
        for (int p : px) {
            float r = (((p >> 16) & 0xFF) / 255f - 0.485f) / 0.229f;
            float g = (((p >> 8) & 0xFF) / 255f - 0.456f) / 0.224f;
            float b = (((p) & 0xFF) / 255f - 0.406f) / 0.225f;
            buf.putFloat(r).putFloat(g).putFloat(b);
        }
        buf.rewind();
        return buf;
    }

}
