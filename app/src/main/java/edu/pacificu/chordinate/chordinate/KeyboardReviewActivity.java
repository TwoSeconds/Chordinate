package edu.pacificu.chordinate.chordinate;

import android.app.Dialog;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import edu.pacificu.chordinate.chordinate.algorithm.Algorithm;

public class KeyboardReviewActivity extends KeyboardActivity implements View.OnTouchListener {

    private Button mRedoButton;
    private Button mSaveAsButton;
    private Button mChordinateButton;
    private Button mPlayButton;
    private EditText mCompName;
    private String mRecordedSong = "";

    public static final String MY_PREFS_NAME = "MyKeyReviewPrefs";
    private static final int START_INDEX = 0;

    private int mNumComps;
    private boolean bChordinated = false;
    private ContextWrapper mContextWrapper = this;

    /**
     * Creates the view for the Keyboard Review Activity
     *
     * @param savedInstanceState    The instance to be created.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keyboard_review);

        Intent reviewIntent = getIntent();
        Bundle extras = reviewIntent.getExtras();
        mRecordedSong = extras.getString("recordedSong");

        mNumComps = 0;
        SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        mNumComps = prefs.getInt("numComps", 0); //0 is the default value

        mRedoButton = (Button) findViewById(R.id.redoButton);
        mRedoButton.setOnTouchListener(this);

        mSaveAsButton = (Button) findViewById(R.id.saveAsIsButton);
        mSaveAsButton.setOnTouchListener(this);

        mPlayButton = (Button) findViewById(R.id.reviewPlay);
        mPlayButton.setOnTouchListener(this);

        mChordinateButton = (Button) findViewById(R.id.chordinateButton);
        mChordinateButton.setOnTouchListener(this);

        mCompName = (EditText) findViewById(R.id.newCompName);
        mCompName.setText("Composition #" + Integer.toString(mNumComps));

    }

    /**
     * Saves the total number of compositions in a shared preferences variable.
     */
    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
        editor.putInt("numComps", mNumComps);
        editor.commit();
    }


    /**
     * Performs the proper actions when a button is pressed
     *
     * @param view      The view being pressed
     * @param event     The touch event last sensed
     */
    public boolean onTouch (View view, MotionEvent event)
    {
        int id = view.getId();

        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            switch (id)
            {
                case R.id.saveAsIsButton:
                    SavedComposition current = new SavedComposition(mNumComps, mCompName.getText().toString(), mRecordedSong);
                    current.writeItemToFile(mContextWrapper);
                    ++mNumComps;
                    Toast.makeText(getApplicationContext(), "Composition Saved", Toast.LENGTH_SHORT).show();

                    finish ();
                    break;

                case R.id.chordinateButton:
                    final Dialog chooseOpts = new Dialog(mContextWrapper);
                    chooseOpts.setContentView(R.layout.choose_comp_options);
                    chooseOpts.show();

                    String[] keys = {"Let us decide", "A", "A#", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#"};
                    ArrayAdapter<String> keysAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, keys);
                    final Spinner keysSpin = (Spinner) chooseOpts.findViewById(R.id.spin_choose_key);
                    keysSpin.setAdapter(keysAdapter);

                    String[] scaleTypes = {"Major", "Natural Minor", "Harmonic Minor", "Melodic Minor"};
                    ArrayAdapter<String> scaleAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, scaleTypes);
                    final Spinner scaleSpin = (Spinner) chooseOpts.findViewById(R.id.spin_choose_scale);
                    scaleSpin.setAdapter(scaleAdapter);

                    Button doneBtn = (Button) chooseOpts.findViewById(R.id.btn_done);
                    doneBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            String key = (String) keysSpin.getSelectedItem();
                            String scaleType = (String) scaleSpin.getSelectedItem();

                            bChordinated = true;
                            chooseOpts.dismiss();

                            String composition = Algorithm.compose(mRecordedSong, key, scaleType, START_INDEX);
                            Log.d("Composition", composition);

                            SavedComposition compToSave = new SavedComposition(mNumComps, mCompName.getText().toString(), composition);
                            compToSave.writeItemToFile(mContextWrapper);
                            ++mNumComps;
                            Toast.makeText(getApplicationContext(), "Composition Saved", Toast.LENGTH_SHORT).show();// TODO: Fix magic constants

                            startCompReviewActivity(compToSave.getName(),
                                    compToSave.getWholeDateStr(), compToSave.getNotes(),
                                    compToSave.getFileName(), true);
                        }
                    });
                    break;

                case R.id.redoButton:
                    finish();
                    break;

                case R.id.reviewPlay:
                    startCompReviewActivity(mCompName.getText().toString(), "160402190427-0700", mRecordedSong, "bogus", false);
                    break;
            }
        }
        return true;
    }

    /**
     * Bundles up necessary variables to pass to the composition review activity and starts that
     * activity.
     *
     * @param compName the name of the composition to be viewed
     * @param dateStr the date of the composition to be viewed
     * @param recordedSong the notes string of the composition to be viewed
     * @param fileName the file name of the composition to be viewed
     * @param bEnableEditMode whether or not edit mode should be enabled in the next activity
     */
    private void startCompReviewActivity(String compName, String dateStr, String recordedSong,
                                         String fileName, boolean bEnableEditMode) {
        Bundle compBundle = new Bundle();
        compBundle.putString("compName", compName);
        compBundle.putString("dateStr", dateStr);
        compBundle.putString("recordedSong", recordedSong);
        compBundle.putString("fileName", fileName);
        compBundle.putBoolean("enableEditMode", bEnableEditMode);

        Intent reviewCompIntent = new Intent(KeyboardReviewActivity.this,
                CompositionViewerActivity.class);
        reviewCompIntent.putExtras(compBundle);
        startActivity(reviewCompIntent);
    }

    /**
     * Overrides onResume. If this activity is resuming after entering the composition viewer,
     * immediately finish this activity.
     */
    @Override
    public void onResume()
    {
        super.onResume();

        if (bChordinated)
        {
            finish();
        }
    }
}
