package com.project.harbor;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.net.Uri;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;

public class ProfileActivity extends AppCompatActivity {

    public static final String PROFILE_PHOTO_NAME = "profile_photo.png";
    private static final int PROFILE_PHOTO_CODE = 12;

    private ImageView profileImg;

    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        profileImg = findViewById(R.id.profileImage);
        profileImg.setImageResource(R.drawable.profil);

        EditText pseudo = findViewById(R.id.textPseudo);
        EditText motto = findViewById(R.id.text_motto);
        TextView scoreMax = findViewById(R.id.textScore);

        user = (User) (getIntent().getSerializableExtra("USER"));

        // set profileImg, pseudo, motto, score to them of user
        if(user != null && !user.isEmpty()) {
            pseudo.setText(user.getPseudo(), TextView.BufferType.EDITABLE);
            motto.setText(user.getMotto(), TextView.BufferType.EDITABLE);
            scoreMax.setText(getString(R.string.text_score)+" "+user.getMaxScore()+" !");
            setProfileImage(user.getUri());
        } else {
            final AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle(R.string.warning);
            alert.setMessage(R.string.empty_profile);
            alert.setIcon(android.R.drawable.ic_dialog_alert);
            alert.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }).show();
        }

        setText(pseudo, 0);
        setText(motto, 1);

        //Change profile image
        profileImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent()
                        .setType("*/*")
                        .setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(intent, "CHANGE_PP"), PROFILE_PHOTO_CODE);
            }
        });

    }

    /**
     * Update user data after editTexts changed
     * @param editText the one changed
     * @param method 0 for update user pseudo, 1 for user motto
     */
    private void setText(EditText editText, final int method) {
        editText.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {}

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(!s.toString().isEmpty()) {
                    if(method==0) user.setPseudo(s.toString());
                    else user.setMotto(s.toString());
                }
            }
        });
    }


    /**
     * Set the profile image to the new one
     * If an exception occurred, update the profile image to the default image
     * @param path : path for the user image
     * @return false if update failed
     */
    public boolean setProfileImage(String path) {
        Bitmap bitmap;
        try {
            Uri uri = Uri.fromFile(new File(path));
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            profileImg.setBackgroundResource(0);
            profileImg.setImageBitmap(bitmap);
            return true;
        } catch (Exception e) {
            Log.e("UPDATE PROFILE IMAGE", "Failed to set profile image", e);
            profileImg.setImageResource(R.drawable.profil);
            return false;
        }
    }



    /**
     * Update the profile image after this one has been changed by the user
     * @param uri : uri of the new image
     */
    private void updateProfileImage(Uri uri) {
        Bitmap bitmap;
        try {
            // Copying the image in the app directory
            File newImg = new File(getFilesDir(), PROFILE_PHOTO_NAME);
            if(!newImg.exists()) newImg.createNewFile();
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            FileOutputStream fos;
            fos = new FileOutputStream(newImg);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            //Setting the image view to the new one
            if(setProfileImage(newImg.getPath())) {
                //This permits us to don't stock the uri of user image. So we don't need the param Uri on User class.
                // But here, we will keep it for portable code (to reuse code one day, on another project)
                user.setUri(newImg.getPath());
            } else {
                //Alert to the failed of the update
                final AlertDialog.Builder alert = new AlertDialog.Builder(ProfileActivity.this);
                alert.setTitle(R.string.warning);
                alert.setMessage(R.string.image_failed);
                alert.setIcon(android.R.drawable.ic_dialog_alert);
                alert.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        }).show();
            }

        } catch (Exception e) {
            Log.e("UPDATE PROFILE IMAGE", "Failed to update profile image", e);
        }

    }


    /**
     * To update data for data changed from up level Activity
     * @param requestCode Activity code
     * @param resultCode Data changed or not
     * @param data data to update
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PROFILE_PHOTO_CODE && resultCode == RESULT_OK) {
            Uri selectedImg = data.getData(); //The uri with the location of the new image
            updateProfileImage(selectedImg);
        }
    }


    /**
     * To update user info in MainActivity
     */
    @Override
    public void onBackPressed() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("USER", user);
        setResult(AppCompatActivity.RESULT_OK, returnIntent);
        finish();
    }
}
