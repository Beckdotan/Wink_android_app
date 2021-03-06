package com.example.wink;

import static android.content.ContentValues.TAG;
import static com.example.wink.SQLiteDBHelper.TABLE_NAME;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Iterator;

//// ----------------------
// was done using this guide: https://www.youtube.com/watch?v=bA7v1Ubjlzw
//// ----------------------

public class NotesReceivingService extends Service {

    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        final ArrayList<UploadImage> notes = new ArrayList<>();
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://wink-e1b43-default-rtdb.europe-west1.firebasedatabase.app/");
        DatabaseReference myRef = database.getReference("Images");

        //getting lists of Notes and making marker from each one of them and present the markers.
        //setting up listener
        ///preparing list from all relevant notes from servers in a list

        //getting refrense to firebase realtime db.
        Log.e("Service ", "Notes Reciving service is running ....  ");


        // -----------------------------------------------

        //   SYNC BETWEEN FIREBASE REALTIME AND LOCAL DB

        // -----------------------------------------------

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.i("Service", "ON DATA CHANGE  ");
                notes.clear();

                for (DataSnapshot noteSnapshot : dataSnapshot.getChildren()) {

                    Log.i("Service", "ON DATA CHANGE in for  " + noteSnapshot.child("id").getValue().toString() );
                    try {

                        //create UploadImage from the firebase data
                        String name = noteSnapshot.child("imageName").getValue().toString();
                        String path = noteSnapshot.child("imagePath").getValue().toString();
                        String id = noteSnapshot.child("id").getValue().toString();
                        String showTime = noteSnapshot.child("showTimeInMillis").getValue().toString();
                        /*
                        // debugging
                        Log.i(TAG, "onDataChange: path = " + path );
                        Log.i(TAG, "onDataChange: id = " +  id );
                        Log.i(TAG, "onDataChange: name = " +  name );
                        //UploadImage currentNote =new UploadImage(name, path, id, "0");
                         */
                        UploadImage currentNote =new UploadImage(name, path, id, showTime);
                        Log.i(TAG, "onDataChange: " + noteSnapshot.getValue().toString());

                        //adding the UploadImage to notes list.
                        notes.add(currentNote);
                        Log.i("onDataChange", "added " + currentNote.getId());
                    } catch (Exception e) { //might happen if we dont add proper notes to the DB.
                        Log.i("DB to UploadImg", "something went wrong with converting DB to Notes" + e);
                    }
                }
                //adding new notes from a list to the local DB.
                try {
                    for (UploadImage note : notes) {
                        Log.i("note list", "now checking " + note.getId());

                        SQLiteDBHelper myDB = new SQLiteDBHelper(NotesReceivingService.this);
                        //TODO: !!!!! check if its for this user as well after implementing users verification
                        if (!myDB.isInLocalDB(note.getId())) {
                            Log.i("Note Receive Service", "Added his note " + note.getId() + "to Local DB");
                            //adding the new note to local DB
                            myDB.addImg(note.getId(), note);
                            //setting up the time for the notification
                            setNotificationInTime(note.getShowTimeInMillis(), note.getId());
                        }
                    }
                } catch (Exception e) {
                    Log.e("On Data Change", "error", e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.i(TAG, "onCancelled: error reading from firebase");
            }
        });

        //OUT OF LISTENER

        //creating the channel.
        final String CHANNEL_ID = "Notes Receiving FS ID";
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_ID,
                NotificationManager.IMPORTANCE_HIGH
        );



        //creating the service.
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        Notification.Builder notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentText("Note Reciving service working")
                .setContentTitle("Service enabled")
                .setSmallIcon(R.drawable.ic_launcher_background);


        //start the service.
        startForeground(1001, notification.build());


        return super.onStartCommand(intent, flags, startId);
    }



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void setNotificationInTime(String timeString, String id){
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationBroadcastReciver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0 , intent, 0);

        long timeforsend = Long.parseLong(timeString);

        alarmManager.set(AlarmManager.RTC_WAKEUP,
                timeforsend,
                pendingIntent);


        Log.i(TAG, "setNotificationInTime: ALARM SET SUCCESSFULLY");

        //adding the image details that needed to the q in the right order.
        addImgToQ(id, Long.parseLong(timeString));


    }

    private void addImgToQ(String id, long time) {
        LinkedListNode node = new LinkedListNode(id, time);

        if (RecivedNoteActivity.q.isEmpty()){
            RecivedNoteActivity.q.add(node);
        }else{
            int counter = 0;
            Iterator iterator = RecivedNoteActivity.q.iterator();
            while(iterator.hasNext() && ((LinkedListNode) iterator.next()).getTimeInMillis() < time){
                counter ++;
            }
            RecivedNoteActivity.q.add(counter, node);
        }
        Log.i(TAG, "setNotificationInTime: the node was added to the q");
    }


}
