package com.bignerdranch.android.notekeeper;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.List;

public class NoteActivity extends AppCompatActivity {
    public final String TAG = getClass().getSimpleName();
    public static final String NOTE_POSITION = "com.bignerdranch.android.notekeeper.NOTE_POSITION";
    public static final String ORIGINAL_NOTE_COURSE_ID = "com.bignerdranch.android.notekeeper.ORIGINAL_NOTE_COURSE_ID";
    public static final String ORIGINAL_NOTE_COURSE_TITLE = "com.bignerdranch.android.notekeeper.ORIGINAL_NOTE_COURSE_TITLE";
    public static final String ORIGINAL_NOTE_TEXT = "come.bignerdranch.android.notekeeper.ORIGINAL_NOTE_COURSE_TEXT";
    public static final int POSITION_NOT_SET = -1;
    private NoteInfo mNote;
    private boolean mIsNewNote;
    private Spinner mSpinnerCourses;
    private EditText mTextNoteTitle;
    private EditText mTextNoteText;
    private int mNewNotePosition;
    private boolean mIsCancelling;
    private String mOriginalNoteCourseId;
    private String mOriginalNoteText;
    private String mOriginalNoteTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSpinnerCourses = (Spinner) findViewById(R.id.spinner_courses);

        List<CourseInfo> courses = DataManager.getInstance().getCourses();
        ArrayAdapter<CourseInfo> adapterCourses = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, courses);
        adapterCourses.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerCourses.setAdapter(adapterCourses);

        readDisplayStateValues();

        if(savedInstanceState == null) {
            saveOriginalNoteValues();
        } else {
            restoreOriginalNoteValues(savedInstanceState);
        }

        mTextNoteTitle = (EditText) findViewById(R.id.text_note_title);
        mTextNoteText = (EditText) findViewById(R.id.text_note_text);

        if(!mIsNewNote) {
            displayNote(mSpinnerCourses, mTextNoteTitle, mTextNoteText);
        }
        Log.d(TAG, "onCreate");
    }

    // Restore the Activity State by retrieving the data put into the Bundle
    private void restoreOriginalNoteValues(Bundle savedInstanceState) {
        mOriginalNoteCourseId = savedInstanceState.getString(ORIGINAL_NOTE_COURSE_ID);
        mOriginalNoteTitle = savedInstanceState.getString(ORIGINAL_NOTE_COURSE_TITLE);
        mOriginalNoteText = savedInstanceState.getString(ORIGINAL_NOTE_TEXT);
    }

    // Save the original note
    private void saveOriginalNoteValues() {
        if(mIsNewNote){
            return;
        }
        mOriginalNoteCourseId = mNote.getCourse().getCourseId();
        mOriginalNoteTitle = mNote.getTitle();
        mOriginalNoteText = mNote.getText();
    }

    // Display the note to the UI
    private void displayNote(Spinner spinnerCourses, EditText textNoteTitle, EditText textNoteText) {
        List<CourseInfo> courses = DataManager.getInstance().getCourses();
        int courseIndex = courses.indexOf(mNote.getCourse());
        spinnerCourses.setSelection(courseIndex);
        textNoteTitle.setText(mNote.getTitle());
        textNoteText.setText(mNote.getText());
    }

    // Read the data from the intent to display on UI
    private void readDisplayStateValues() {
        Intent intent = getIntent();
        mNewNotePosition = intent.getIntExtra(NOTE_POSITION, POSITION_NOT_SET);

        mIsNewNote = mNewNotePosition == POSITION_NOT_SET;

        if(mIsNewNote) {
            createNewNote();
        }
        Log.i(TAG, "mNotePosition: " + mNewNotePosition);
        mNote = DataManager.getInstance().getNotes().get(mNewNotePosition);
    }

    // Create a new note
    private void createNewNote() {
        DataManager dm = DataManager.getInstance();
        mNewNotePosition = dm.createNewNote();
        //mNote = dm.getNotes().get(mNewNotePosition);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_note, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_send_mail) {
            sendEmail();
            return true;
        } else if (id == R.id.action_cancel){
            mIsCancelling = true;
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // If the user cancels we will first see if its a new note
        // If it is a new new we will remove the new note
        // If not we will restore the old note
        // If the user is not cancelling we will save the note
        if(mIsCancelling) {
            Log.i(TAG, "Cancelling Note at position: " + mNewNotePosition);
            if(mIsNewNote) {
                DataManager.getInstance().removeNote(mNewNotePosition);
            } else {
                storePreviousNoteValues();
            }
        } else {
            saveNote();
        }
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ORIGINAL_NOTE_COURSE_ID, mOriginalNoteCourseId);
        outState.putString(ORIGINAL_NOTE_COURSE_TITLE, mOriginalNoteTitle);
        outState.putString(ORIGINAL_NOTE_TEXT, mOriginalNoteText);
    }

    // Restores previous note values
    private void storePreviousNoteValues() {
        CourseInfo course = DataManager.getInstance().getCourse(mOriginalNoteCourseId);
        mNote.setCourse(course);
        mNote.setTitle(mOriginalNoteTitle);
        mNote.setText(mOriginalNoteText);
    }

    // Saves the note
    private void saveNote() {
        mNote.setCourse((CourseInfo) mSpinnerCourses.getSelectedItem());
        mNote.setTitle(mTextNoteTitle.getText().toString());
        mNote.setText(mTextNoteText.getText().toString());
    }

    // Creates an Intent and starts an email Activity
    private void sendEmail() {
        CourseInfo course = (CourseInfo) mSpinnerCourses.getSelectedItem();
        String subject = mTextNoteTitle.getText().toString();
        String text = "Checkout what I learned in the Pluralsight course \"" + course.getTitle() + "\"\n" + mTextNoteText.getText().toString();

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc2822");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(intent);
    }
}
