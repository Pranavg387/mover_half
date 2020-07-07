package com.example.mover_f;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class customerLoginActivity extends AppCompatActivity {

    private Button mlogin, mregister;
    private EditText memail, mpassword;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;
    DatabaseReference databaseUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_login);
        databaseUsers = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        firebaseAuthListener= new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user!=null){
                    Intent intent = new Intent(customerLoginActivity.this, customerMapsActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                }
            }
        };

        memail = findViewById(R.id.email);
        mpassword= findViewById(R.id.password);
        mlogin= findViewById(R.id.login);
        mregister= findViewById(R.id.register);

        mregister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email  = memail .getText().toString();
                final String password  = mpassword .getText().toString();
                mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(customerLoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()){
                            Toast.makeText(customerLoginActivity.this,"Sign up Error",Toast.LENGTH_SHORT).show();
                        }else
                        {
                            String user_id = mAuth.getCurrentUser().getUid();

                            String id = databaseUsers.push().getKey();
                            databaseUsers.child("Users").child("Customers").setValue(id);
                        }
                    }
                });
            }
        });
        mlogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email  = memail .getText().toString();
                final String password  = mpassword .getText().toString();
                mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(customerLoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()){
                            Toast.makeText(customerLoginActivity.this,"Sign in Error",Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            }
        });



    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(firebaseAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.addAuthStateListener(firebaseAuthListener);
    }
}