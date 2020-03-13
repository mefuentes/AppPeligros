package com.alain.cursos.multilogin;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;



public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 123;
    private static final int RC_FROM_GALLERY = 124;

    private static final String PROVEEDOR_DESCONOCIDO = "Proveedor desconocido";
    private static final String PASSWORD_FIREBASE = "password";
    private static final String GOOGLE = "google.com";


    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    private Button btnSiguiente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        btnSiguiente = findViewById(R.id.btnSiguiente);

        dameUbicacion();

        mFirebaseAuth = FirebaseAuth.getInstance();

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    onSetDataUser(user.getDisplayName(), user.getEmail(), user.getProviders() != null ?
                            user.getProviders().get(0) : PROVEEDOR_DESCONOCIDO);

                    loadImage(user.getPhotoUrl());
                } else {
                    onSignedOutCleanup();

                    AuthUI.IdpConfig facebookIdp = new AuthUI.IdpConfig.FacebookBuilder()
                            .setPermissions(Arrays.asList("user_friends", "user_gender"))
                            .build();

                    AuthUI.IdpConfig googleIdp = new AuthUI.IdpConfig.GoogleBuilder()
                            //.setScopes(Arrays.asList(Scopes.GAMES))
                            .build();

                    startActivityForResult(AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setIsSmartLockEnabled(false)
                            .setTosUrl("http://databaseremote.esy.es/RegisterLite/html/privacidad.html")
                            .setAvailableProviders(Arrays.asList(new AuthUI.IdpConfig.EmailBuilder().build(),
                                    facebookIdp, googleIdp))
                            .setTheme(R.style.GreenTheme)
                            .setLogo(R.drawable.img_multi_login)
                            .build(), RC_SIGN_IN);
                }
            }

        };

        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    "com.alain.cursos.multilogin",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {

        } catch (NoSuchAlgorithmException e) {

        }
    }

    private void dameUbicacion() {

        btnSiguiente.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, UbicacionActivity.class);
                startActivityForResult(intent, 0);
            }
        });
    }

    private void onSignedOutCleanup() {
        onSetDataUser("", "", "");
    }

    private void onSetDataUser(String userName, String email, String provider) {

        int drawableRes;
        switch (provider) {
            case GOOGLE:
                drawableRes = R.drawable.ic_google_plus_box;
                break;
            default:
                drawableRes = R.drawable.ic_block_helper;
                provider = PROVEEDOR_DESCONOCIDO;
                break;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bienvenido...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Algo falló, intente de nuevo.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == RC_FROM_GALLERY && resultCode == RESULT_OK){
            FirebaseStorage storage = FirebaseStorage.getInstance();
            Uri selectedImageUri = data.getData();

        }
    }

    private void loadImage(Uri photoUrl) {
        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop();

        Glide.with(MainActivity.this)
                .load(photoUrl) //+ "?type=large")//+ "?height=500")
                .apply(options);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sign_out:
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
