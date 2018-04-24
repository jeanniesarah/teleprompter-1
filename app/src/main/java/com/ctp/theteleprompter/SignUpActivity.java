package com.ctp.theteleprompter;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.ctp.theteleprompter.data.SharedPreferenceUtils;
import com.ctp.theteleprompter.services.DocService;
import com.ctp.theteleprompter.utils.TeleUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SignUpActivity extends AppCompatActivity
                implements View.OnClickListener{

    private static final String TAG = SignUpActivity.class.getSimpleName();

    @BindView(R.id.email)
    EditText emailInput;

    @BindView(R.id.password)
    EditText passwordInput;

    @BindView(R.id.confirm_password)
    EditText confirmPasswordInput;

    @BindView(R.id.full_name)
    EditText fullNameInput;

    @BindView(R.id.sign_up_button)
    Button signUpButton;

    @BindView(R.id.sign_up_container)
    LinearLayout layoutContainer;



    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        ButterKnife.bind(this);
        mAuth = FirebaseAuth.getInstance();
        signUpButton.setOnClickListener(this);

    }



    @Override
    public void onClick(View view) {
        int id = view.getId();

        switch (id){
            case R.id.sign_up_button:
                initiateSignUp();
                break;
        }
    }


    private void initiateSignUp(){
        signUpButton.setEnabled(false);
        String name = fullNameInput.getText().toString();
        String email = emailInput.getText().toString();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        if(!TeleUtils.isConnectedToNetwork(this)){
            Snackbar.make(layoutContainer,
                    "Check your internet connection"
                    ,Snackbar.LENGTH_LONG).show();

            return;
        }

        boolean cancel = false;
        View focusView = null;

        if(name.trim().isEmpty()){
            fullNameInput.setError(getString(R.string.error_field_required));
            focusView = fullNameInput;
            cancel =true;
        }

        if(name.length()>60){
            fullNameInput.setError(getString(R.string.error_long_name));
            focusView = fullNameInput;
            cancel =true;
        }

        if (TextUtils.isEmpty(email)) {
            emailInput.setError(getString(R.string.error_field_required));
            focusView = emailInput;
            cancel = true;

        } else if (!TeleUtils.isValidEmail(email)) {
            emailInput.setError(getString(R.string.error_invalid_email));
            focusView = emailInput;
            cancel = true;
        }

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password) && TeleUtils.isValidPassword(password)) {
            passwordInput.setError(getString(R.string.error_invalid_password));
            focusView = passwordInput;
            cancel = true;
        }


        if(TextUtils.isEmpty(confirmPassword) && TeleUtils.isValidPassword(confirmPassword)){
            confirmPasswordInput.setError(getString(R.string.error_invalid_password));
            focusView = confirmPasswordInput;
            cancel = true;
        }

        if(!password.equals(confirmPassword)){
            confirmPasswordInput.setError(getString(R.string.error_password_nomatch));
            passwordInput.setError(getString(R.string.error_password_nomatch));
            focusView = confirmPasswordInput;
            cancel = true;

        }// Check for a valid email address.



        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.

            if (!TeleUtils.isConnectedToNetwork(this)) {
//                TODO: launch a dialog
                Snackbar.make(layoutContainer, "Check your internet connection", Snackbar.LENGTH_LONG).show();
            }
            else {

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Sign in success, update UI with the signed-in user's information
//                            Log.d(TAG, "createUserWithEmail:success");
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    updateUI(user);
                                } else {
                                    // If sign in fails, display a message to the user.
                                   Exception exception = task.getException();

                                   if(exception instanceof FirebaseAuthUserCollisionException){

                                       Toast.makeText(SignUpActivity.this, "Account already exists!" +
                                                       "Please Sign in",
                                               Toast.LENGTH_SHORT).show();
                                   }else {
                                       Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                       Toast.makeText(SignUpActivity.this, "Authentication failed.",
                                               Toast.LENGTH_SHORT).show();

                                   }
                                    updateUI(null);
                                }

                                // ...
                            }
                        });

            }

        }




    }

    private void updateProfileInfo(final FirebaseUser user){


        final String name = fullNameInput.getText().toString();
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();


        user.updateProfile(profileUpdates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {

                            SharedPreferenceUtils.setPrefUsername(SignUpActivity.this,name);
                            SharedPreferenceUtils.setPrefUserId(SignUpActivity.this,user.getUid());
                            SharedPreferenceUtils.setPrefEmail(SignUpActivity.this,user.getEmail());
                            DocService.syncDocs(SignUpActivity.this,user.getUid());
                            Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                            startActivity(intent);
                        }
                    }
                });

    }



    private void updateUI(FirebaseUser currentUser){

        if(currentUser == null){
//            TODO: some error occured
            signUpButton.setEnabled(true);
            return;

        }
        else {
            updateProfileInfo(currentUser);
//            TODO: store id in shared preferences
        }



    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Intent intent = new Intent(SignUpActivity.this,LoginActivity.class);
        startActivity(intent);
    }
}