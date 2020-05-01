package com.server.fooddoor;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.github.ybq.android.spinkit.style.WanderingCubes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.server.fooddoor.model.Category;
import com.squareup.picasso.Picasso;
import com.tapadoo.alerter.Alerter;

import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class MenuActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private CircleImageView profilePic;
    private TextView username;
    private Button addCategory;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private FirebaseUser user;
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase database;

    private DatabaseReference category;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private RecyclerView recycler_menu;
    private RecyclerView.LayoutManager layoutManager;
    FirebaseRecyclerAdapter<Category, MenuViewHolder> adapter;
    String name, email;

    private TextView wait1, wait2;
    private ProgressBar progressBar;

    //for dialog
    ImageView closeDialog;
    TextInputLayout categoryName, categoryDiscount;
    EditText txtName, txtDiscount;
    Button selectPicture, uploadImage, uploadData;

    Category newCategory;
    Uri imageUri;
    private final int PICK_IMG_REQUEST = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        Toolbar toolbar = findViewById(R.id.menu_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
        toolbarTitle.setText(R.string.ForMenuTitle);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        NavigationView navigationView = findViewById(R.id.nav_view);
        assert navigationView != null;
        navigationView.setNavigationItemSelectedListener(this);

        profilePic = navigationView.getHeaderView(0).findViewById(R.id.pro_pic);
        username = navigationView.getHeaderView(0).findViewById(R.id.user_name);

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();

        database = FirebaseDatabase.getInstance();
        category = database.getReference("Category");
        category.keepSynced(true);

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        wait1 = findViewById(R.id.wait1);
        wait2 = findViewById(R.id.wait2);

        progressBar = findViewById(R.id.WanderingCubes);
        WanderingCubes wanderingCubes = new WanderingCubes();
        progressBar.setIndeterminateDrawable(wanderingCubes);

        addCategory = findViewById(R.id.addCategory);

        if(user == null){
            username.setText("LOGIN/SIGNUP");
            username.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MenuActivity.this, LoginActivity.class));
                    finish();
                }
            });
        } else if(user != null){
            for(UserInfo profile : user.getProviderData()){
                String providerId = profile.getProviderId();

                String uid = profile.getUid();

                String name = profile.getDisplayName();
                String email = profile.getEmail();
                Uri photoUrl = profile.getPhotoUrl();

                Glide.with(this)
                        .load(photoUrl)
                        .into(profilePic);

                username.setText("Hi, " + name);
            }
        }

        addCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddDialog();
            }
        });

        recycler_menu = findViewById(R.id.recycler_menu);
        recycler_menu.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recycler_menu.setLayoutManager(layoutManager);

        menuloader();
    }

    private void showAddDialog() {
        final AlertDialog alertDialog = new AlertDialog.Builder(MenuActivity.this).create();

        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.add_category_dialog, null);

        closeDialog = view.findViewById(R.id.closeDialog);
        categoryName = view.findViewById(R.id.categoryName);
        categoryDiscount = view.findViewById(R.id.categoryDiscount);
        txtName = view.findViewById(R.id.txtName);
        txtDiscount = view.findViewById(R.id.txtDiscount);
        selectPicture = view.findViewById(R.id.selectPicture);
        uploadImage = view.findViewById(R.id.uploadImage);
        uploadData = view.findViewById(R.id.uploadCategory);

        alertDialog.setView(view);

        closeDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        selectPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage();     //let user select image from gallery and save its Uri
            }
        });

        uploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImage();
            }
        });

        uploadData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                if(newCategory != null)
                {
                    category.push().setValue(newCategory);
                    Alerter.create(MenuActivity.this)
                            .setTitle("NEW CATEGORY ADDED!")
                            .setText("A new Category " + newCategory.getName() + " was added. Check it Below!")
                            .setTextAppearance(R.style.Alerter1TextAppearance)
                            .setBackgroundColorRes(R.color.Alerter1)
                            .setIcon(R.drawable.ic_check_circle_black_24dp)
                            .setDuration(3000)
                            .enableSwipeToDismiss()
                            .enableIconPulse(true)
                            .enableVibration(true)
                            .show();
                }
                else {
                    Alerter.create(MenuActivity.this)
                            .setTitle("SOME ERROR OCCURRED!")
                            .setTextAppearance(R.style.Alerter1TextAppearance)
                            .setBackgroundColorRes(R.color.Alerter1)
                            .setIcon(R.drawable.ic_error_black_24dp)
                            .setDuration(3000)
                            .enableSwipeToDismiss()
                            .enableIconPulse(true)
                            .enableVibration(true)
                            .show();
                }
            }
        });

        alertDialog.show();
    }

    private void uploadImage() {
        if(imageUri != null)
        {
            final ProgressDialog progressDialog = new ProgressDialog(MenuActivity.this);
            progressDialog.setMessage("Uploading...");
            progressDialog.show();

            String imageName = UUID.randomUUID().toString();
            final StorageReference imageFolder = storageReference.child("Category/" + imageName);

            imageFolder.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss();
                            Toast.makeText(MenuActivity.this, "Upload Successful!", Toast.LENGTH_SHORT).show();

                            imageFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    newCategory = new Category(txtName.getText().toString().trim(), uri.toString(), txtDiscount.getText().toString().trim());
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(MenuActivity.this, "Upload Unsuccessful!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                            progressDialog.setMessage("Uploading..." + progress + "%");
                        }
                    });
        }
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Category Image"), PICK_IMG_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMG_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null)
        {
            imageUri = data.getData();
            selectPicture.setText("SELECTED");
            selectPicture.setEnabled(false);
            selectPicture.setBackgroundResource(R.drawable.round_layout4);
            uploadImage.setEnabled(true);
            uploadImage.setBackgroundResource(R.drawable.round_layout3);
        }
    }

    private void menuloader() {
        adapter = new FirebaseRecyclerAdapter<Category, MenuViewHolder>(Category.class, R.layout.menu_cards, MenuViewHolder.class, category)
        {
            @Override
            protected void populateViewHolder(MenuViewHolder viewHolder, Category model, final int position) {
                viewHolder.setMenuName(model.getName());
                viewHolder.setMenuImage(getApplicationContext(), model.getImage());
                viewHolder.setDiscount(model.getDiscount());
                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        Intent foodList = new Intent(MenuActivity.this, FoodListActivity.class);
                        foodList.putExtra("CategoryId", adapter.getRef(position).getKey());
                        startActivity(foodList);
                    }
                });

                viewHolder.update.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showUpdateDialog(adapter.getRef(position).getKey(), adapter.getItem(position));
                    }
                });

                viewHolder.delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteCategory(adapter.getRef(position).getKey());
                    }
                });
            }

            @Override
            protected void onDataChanged() {
                if(progressBar != null && wait1 != null && wait2 != null){
                    progressBar.setVisibility(View.GONE);
                    wait1.setVisibility(View.GONE);
                    wait2.setVisibility(View.GONE);
                }
            }
        };

        adapter.notifyDataSetChanged();  //show updated data
        recycler_menu.setAdapter(adapter);
    }

    private void deleteCategory(final String key) {
        AlertDialog.Builder sure = new AlertDialog.Builder(MenuActivity.this);
        sure.setMessage("Are you sure you want to Delete this Category?");
        sure.setCancelable(false);
        sure.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                DatabaseReference foods = database.getReference("Foods");
                Query foodInCategory = foods.orderByChild("menuId").equalTo(key);
                foodInCategory.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot postSnapshot : dataSnapshot.getChildren())
                        {
                            postSnapshot.getRef().removeValue();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                category.child(key).removeValue();
                Alerter.create(MenuActivity.this)
                        .setTitle("A CATEGORY HAS BEEN DELETED!")
                        .setTextAppearance(R.style.Alerter1TextAppearance)
                        .setBackgroundColorRes(R.color.Alerter1)
                        .setIcon(R.drawable.ic_check_circle_black_24dp)
                        .setDuration(3000)
                        .enableSwipeToDismiss()
                        .enableIconPulse(true)
                        .enableVibration(true)
                        .show();
            }
        });

        sure.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alertDialog = sure.create();
        alertDialog.show();
    }

    private void showUpdateDialog(final String key, final Category item) {
        final AlertDialog dialog = new AlertDialog.Builder(MenuActivity.this).create();

        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.add_category_dialog, null);

        closeDialog = view.findViewById(R.id.closeDialog);
        categoryName = view.findViewById(R.id.categoryName);
        categoryDiscount = view.findViewById(R.id.categoryDiscount);
        txtName = view.findViewById(R.id.txtName);
        txtDiscount = view.findViewById(R.id.txtDiscount);
        selectPicture = view.findViewById(R.id.selectPicture);
        uploadImage = view.findViewById(R.id.uploadImage);
        uploadData = view.findViewById(R.id.uploadCategory);

        txtName.setText(item.getName());
        txtDiscount.setText(item.getDiscount());

        dialog.setView(view);

        closeDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        selectPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage();     //let user select image from gallery and save its Uri
            }
        });

        uploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeImage(item);
            }
        });

        uploadData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();

                item.setName(txtName.getText().toString().trim());
                item.setDiscount(txtDiscount.getText().toString().trim());
                category.child(key).setValue(item);

                Alerter.create(MenuActivity.this)
                        .setTitle("CATEGORY UPDATED!")
                        .setText(item.getName() + " was Updated. Check it Below!")
                        .setTextAppearance(R.style.Alerter1TextAppearance)
                        .setBackgroundColorRes(R.color.Alerter1)
                        .setIcon(R.drawable.ic_check_circle_black_24dp)
                        .setDuration(3000)
                        .enableSwipeToDismiss()
                        .enableIconPulse(true)
                        .enableVibration(true)
                        .show();
            }
        });

        dialog.show();
    }

    private void changeImage(final Category item) {
        if(imageUri != null)
        {
            final ProgressDialog progressDialog = new ProgressDialog(MenuActivity.this);
            progressDialog.setMessage("Uploading...");
            progressDialog.show();

            String imageName = UUID.randomUUID().toString();
            final StorageReference imageFolder = storageReference.child("Category/" + imageName);

            imageFolder.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss();
                            Toast.makeText(MenuActivity.this, "Upload Successful!", Toast.LENGTH_SHORT).show();

                            imageFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    item.setImage(uri.toString());
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(MenuActivity.this, "Upload Unsuccessful!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                            progressDialog.setMessage("Uploading..." + progress + "%");
                        }
                    });
        }
    }

    public interface ItemClickListener{
        void onClick(View view, int position, boolean isLongClick);
    }

    public static class MenuViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        View view;
        Button update, delete;

        private ItemClickListener itemClickListener;

        public MenuViewHolder(@NonNull View itemView) {
            super(itemView);
            view = itemView;

            update = view.findViewById(R.id.updateCategory);
            delete = view.findViewById(R.id.deleteCategory);

            itemView.setOnClickListener(this);
        }

        public void setMenuName(String menuName){
            TextView txtMenuName = view.findViewById(R.id.menu_name);
            txtMenuName.setText(menuName);
        }

        public void setMenuImage(Context context, String menuImage){
            ImageView MenuImage = view.findViewById(R.id.menu_img);
            Picasso.get().load(menuImage).into(MenuImage);
        }

        public void setDiscount(String discount){
            TextView Discount = view.findViewById(R.id.discount);
            Discount.setText(discount);
        }

        public void setItemClickListener(ItemClickListener itemClickListener) {
            this.itemClickListener = itemClickListener;
        }

        @Override
        public void onClick(View v) {
            itemClickListener.onClick(v, getAdapterPosition(), false);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(drawerToggle.onOptionsItemSelected(item)){
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

        int id = menuItem.getItemId();

        if(id == R.id.nav_home){
            startActivity(new Intent(MenuActivity.this, HomeActivity.class));
        }

        if(id == R.id.nav_menu){
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }

        if(id == R.id.nav_cart){
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }

        if(id == R.id.nav_about){
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }

        if(id == R.id.nav_orders){
            startActivity(new Intent(MenuActivity.this, OrderStatus.class));
        }

        if(id == R.id.nav_rate){
            startActivity(new Intent(MenuActivity.this, OrderStatus.class));
        }

        if(id == R.id.nav_share){
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }

        if(id == R.id.nav_track){
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }

        if(id == R.id.nav_notifications){
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }

        if(id == R.id.nav_logout){
            AlertDialog.Builder builder = new AlertDialog.Builder(MenuActivity.this);
            builder.setMessage("Are you sure you want to Sign Out?");
            builder.setCancelable(false);
            builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    AuthUI.getInstance()
                            .signOut(MenuActivity.this)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    startActivity(new Intent(MenuActivity.this, LoginActivity.class));
                                    finish();
                                }
                            });
                }
            });

            builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            AlertDialog alertDialog = builder.create();
            alertDialog.show();

        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
