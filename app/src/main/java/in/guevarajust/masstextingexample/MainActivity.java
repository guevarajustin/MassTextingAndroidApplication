package in.guevarajust.masstextingexample;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.opencsv.CSVReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class MainActivity extends AppCompatActivity {

    Button start_button;
    SmsManager smsManager;
    TextView status;
    TextView feedback;
    CSVReader reader;
    String[] nextLine;
    String message;

    @Override
    protected void onStart() {
        super.onStart();

        verifyStoragePermissions(MainActivity.this); // having this line in onStart() means permissions are only obtained once -after activity is created
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = (TextView) findViewById(R.id.textView1);
        feedback = (TextView) findViewById(R.id.textView2);
        start_button = (Button) findViewById(R.id.button1);
        smsManager = SmsManager.getDefault();

        start_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                MainActivity.this.line_number = 1;
                MainActivity.this.start_button.setEnabled(false);
                MainActivity.this.feedback.setText("");

                // required files (phone_numbers.csv, message.txt) stored at storage root directory for most android devices
                File primary_storage_root_path = Environment.getExternalStorageDirectory();
                File phone_numbers_file_path = new File(primary_storage_root_path,"phone_numbers.csv");
                File message_file_path = new File(primary_storage_root_path,"message.txt");

                try {
                    StringBuilder text = new StringBuilder();
                    BufferedReader br = new BufferedReader(new FileReader(message_file_path));
                    String line;
                    while ((line = br.readLine()) != null) {
                        text.append(line);
                        text.append('\n');
                    }
                    br.close();
                    MainActivity.this.message = text.toString();

                    if (MainActivity.this.message.length() < 161){

                        MainActivity.this.reader = new CSVReader(new FileReader(phone_numbers_file_path), ',');
                        send_text_after_waiting_period_of_time();

                    } else {

                        feedback.append("Mass text attempt failed. Message retrieved from message.txt was over 160 characters (" + MainActivity.this.message.length() + ")" + "" + System.getProperty ("line.separator"));////////////////
                        MainActivity.this.status.setText("Application status: Disabled. Restart application to make another mass text attempt.");

                    }

                } catch (FileNotFoundException e) {

                    if (e.getMessage().equals("/storage/emulated/0/message.txt: open failed: ENOENT (No such file or directory)")) {
                        feedback.append("Mass text attempt failed. Could not find message.txt" + System.getProperty ("line.separator"));
                    } else if (e.getMessage().equals("/storage/emulated/0/phone_numbers.csv: open failed: ENOENT (No such file or directory)")) {
                        feedback.append("Mass text attempt failed. Could not find phone_numbers.csv" + System.getProperty ("line.separator"));
                    }
                    MainActivity.this.status.setText("Application status: Disabled. Restart application to make another mass text attempt.");

                } catch (Exception e) {

                    MainActivity.this.status.setText("Application status: Disabled. Restart application to make another mass text attempt.");

                }

            }

        });

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    // below is required for permissions in later versions of android
    // https://stackoverflow.com/questions/33719170/android-6-0-file-write-permission-denied
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_SMS,
    };
    // https://stackoverflow.com/questions/33719170/android-6-0-file-write-permission-denied
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );

        }
    }

    int line_number; // int declared here since Java 7 has limited support for closures. Java 8 not supported by current API level
    final int delay_in_millisecond = 2000; // wait time before test message send attempt
    private void send_text_after_waiting_period_of_time () {
        Handler handler = new Handler();
        handler.postDelayed(
            new Runnable() {
                public void run() {
                    try {
                        if ((nextLine = reader.readNext()) != null) {

                            try  {
                                MainActivity.this.smsManager.sendTextMessage(nextLine[0], null, MainActivity.this.message, null, null);
                            } catch (Exception e) {
                                feedback.append("F@" + Integer.toString(MainActivity.this.line_number) + System.getProperty ("line.separator"));
                            }

                            MainActivity.this.line_number++;
                            send_text_after_waiting_period_of_time();

                        } else {
                            feedback.append("Process complete" + System.getProperty ("line.separator"));
                            status.setText("Application status: Disabled. Restart application to make another mass text attempt.");
                        }
                    } catch (Exception e) {
                        feedback.append("Process aborted. Failure occurred for phone number on line: " + Integer.toString(MainActivity.this.line_number) + ". No attempts were made to text phone numbers on list after failure point." + System.getProperty ("line.separator"));
                        status.setText("Application status: Disabled. Restart application to make another mass text attempt.");
                    }
                }
            },
            MainActivity.this.delay_in_millisecond
        );
    }

}