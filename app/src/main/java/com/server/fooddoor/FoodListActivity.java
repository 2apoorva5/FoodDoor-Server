package com.server.fooddoor;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.Fade;
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

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.github.ybq.android.spinkit.style.WanderingCubes;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.server.fooddoor.model.Food;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.squareup.picasso.Picasso;
import com.tapadoo.alerter.Alerter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FoodListActivity extends AppCompatActivity {

    String categoryId = "";
    private FirebaseDatabase database;
    private DatabaseReference foodlist;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private RecyclerView recycler_food;
    private RecyclerView.LayoutManager layoutManager;
    FirebaseRecyclerAdapter<Food, FoodViewHolder> adapter;
    FirebaseRecyclerAdapter<Food, FoodViewHolder> searchAdapter;
    List<String> suggestList = new ArrayList<>();
    MaterialSearchBar materialSearchBar;

    private TextView wait;
    private ProgressBar progressBar;
    private Button addFood;

    ImageView closeDialog;
    TextInputLayout foodName, foodDiscount, foodID, foodPrice, foodPrePrice, foodDesc;
    EditText txtName, txtDiscount, txtID, txtPrice, txtPrePrice, txtDesc;
    Button selectPicture, uploadImage, uploadData;

    Food newFood;
    Uri imageUri;
    private final int PICK_IMG_REQUEST = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_list);

        Toolbar toolbar = findViewById(R.id.food_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
        toolbarTitle.setText(R.string.ForFoodListTitle);
        toolbar.setNavigationIcon(R.drawable.arrow_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        Fade fade = new Fade();
        View decor = getWindow().getDecorView();
        fade.excludeTarget(decor.findViewById(R.id.action_bar_container), true);
        fade.excludeTarget(android.R.id.statusBarBackground, true);
        fade.excludeTarget(android.R.id.navigationBarBackground, true);

        getWindow().setEnterTransition(fade);
        getWindow().setExitTransition(fade);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }

        database = FirebaseDatabase.getInstance();
        foodlist = database.getReference("Foods");
        foodlist.keepSynced(true);

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        recycler_food = findViewById(R.id.recycler_food);
        recycler_food.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recycler_food.setLayoutManager(layoutManager);

        if(getIntent() != null){
            categoryId = getIntent().getStringExtra("CategoryId");
        }
        if(!categoryId.isEmpty() && categoryId != null){
            foodlistloader(categoryId);
        }

        materialSearchBar = findViewById(R.id.foodSearch);
        wait = findViewById(R.id.wait);
        progressBar = findViewById(R.id.WanderingCubes);
        WanderingCubes wanderingCubes = new WanderingCubes();
        progressBar.setIndeterminateDrawable(wanderingCubes);

        addFood = findViewById(R.id.addFood);

        materialSearchBar.setLastSuggestions(suggestList);
        materialSearchBar.setCardViewElevation(10);
        materialSearchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                List<String> suggest = new ArrayList<>();
                for(String search : suggestList){
                    if(search.toLowerCase().contains(materialSearchBar.getText().toLowerCase())){
                        suggest.add(search);
                    }
                }
                materialSearchBar.setLastSuggestions(suggest);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        materialSearchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {
                if(!enabled){
                    recycler_food.setAdapter(adapter);
                }
            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                startSearch(text);
            }

            @Override
            public void onButtonClicked(int buttonCode) {

            }
        });

        addFood.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddFoodDialog();
            }
        });

        loadSuggestion();
    }

    private void showAddFoodDialog() {
        final AlertDialog alertDialog = new AlertDialog.Builder(FoodListActivity.this).create();

        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.add_food_dialog, null);

        closeDialog = view.findViewById(R.id.closeDialog);
        foodName = view.findViewById(R.id.foodName);
        foodDiscount = view.findViewById(R.id.foodDiscount);
        foodID = view.findViewById(R.id.foodID);
        foodPrice = view.findViewById(R.id.foodPrice);
        foodPrePrice = view.findViewById(R.id.foodPrePrice);
        foodDesc = view.findViewById(R.id.foodDesc);
        txtName = view.findViewById(R.id.txtName);
        txtDiscount = view.findViewById(R.id.txtDiscount);
        txtID = view.findViewById(R.id.txtID);
        txtPrice = view.findViewById(R.id.txtPrice);
        txtPrePrice = view.findViewById(R.id.txtPrePrice);
        txtDesc = view.findViewById(R.id.txtDesc);
        selectPicture = view.findViewById(R.id.selectImage);
        uploadImage = view.findViewById(R.id.uploadImage);
        uploadData = view.findViewById(R.id.uploadFood);

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
                chooseImage();
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
                if(newFood != null)
                {
                    foodlist.push().setValue(newFood);
                    Alerter.create(FoodListActivity.this)
                            .setTitle("NEW FOOD ADDED!")
                            .setText("A new Food " + newFood.getName() + " was added. Check it Below!")
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
                    Alerter.create(FoodListActivity.this)
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

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Food Image"), PICK_IMG_REQUEST);
    }

    private void uploadImage() {
        if(imageUri != null)
        {
            final ProgressDialog progressDialog = new ProgressDialog(FoodListActivity.this);
            progressDialog.setMessage("Uploading...");
            progressDialog.show();

            String imageName = UUID.randomUUID().toString();
            final StorageReference imageFolder = storageReference.child("AddedFoods/" + imageName);

            imageFolder.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss();
                            Toast.makeText(FoodListActivity.this, "Upload Successful!", Toast.LENGTH_SHORT).show();

                            imageFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    newFood = new Food();
                                    newFood.setName(txtName.getText().toString().trim());
                                    newFood.setDiscount(txtDiscount.getText().toString().trim());
                                    newFood.setMenuId(txtID.getText().toString().trim());
                                    newFood.setPrice(txtPrice.getText().toString().trim());
                                    newFood.setPreviousPrice(txtPrePrice.getText().toString().trim());
                                    newFood.setDescription(txtDesc.getText().toString().trim());
                                    newFood.setImage(uri.toString());
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(FoodListActivity.this, "Upload Unsuccessful!", Toast.LENGTH_SHORT).show();
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

    private void startSearch(CharSequence text) {
        searchAdapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(Food.class, R.layout.food_cards, FoodViewHolder.class, foodlist.orderByChild("name").equalTo(text.toString())) {
            @Override
            protected void populateViewHolder(final FoodViewHolder viewHolder, Food model, final int position) {
                viewHolder.setFoodName(model.getName());
                viewHolder.setFoodImage(getApplicationContext(), model.getImage());
                viewHolder.setMealType(getApplicationContext(), model.getType());
                viewHolder.setDiscount(model.getDiscount());
                viewHolder.setPrice(model.getPrice());
                viewHolder.setPreviousPrice(model.getPreviousPrice());
                viewHolder.setCross(getApplicationContext(), model.getClose());
                viewHolder.setFoodClickListener(new FoodClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        Toast.makeText(FoodListActivity.this, "Clicked.", Toast.LENGTH_SHORT).show();
                    }
                });

                viewHolder.update.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showUpdateFoodDialog(adapter.getRef(position).getKey(), adapter.getItem(position));
                    }
                });

                viewHolder.remove.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(FoodListActivity.this, "Clicked", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };

        searchAdapter.notifyDataSetChanged();
        recycler_food.setAdapter(searchAdapter);
    }

    private void loadSuggestion() {
        foodlist.orderByChild("menuId").equalTo(categoryId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot postSnapshot : dataSnapshot.getChildren()){
                            Food item = postSnapshot.getValue(Food.class);
                            suggestList.add(item.getName());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void foodlistloader(String categoryId) {
        adapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(Food.class, R.layout.food_cards, FoodViewHolder.class, foodlist.orderByChild("menuId").equalTo(categoryId)) {
            @Override
            protected void populateViewHolder(final FoodViewHolder viewHolder, Food model, final int position) {
                viewHolder.setFoodName(model.getName());
                viewHolder.setFoodImage(getApplicationContext(), model.getImage());
                viewHolder.setMealType(getApplicationContext(), model.getType());
                viewHolder.setDiscount(model.getDiscount());
                viewHolder.setPrice(model.getPrice());
                viewHolder.setPreviousPrice(model.getPreviousPrice());
                viewHolder.setCross(getApplicationContext(), model.getClose());
                viewHolder.setFoodClickListener(new FoodClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        Toast.makeText(FoodListActivity.this, "Clicked", Toast.LENGTH_SHORT).show();
                    }
                });

                viewHolder.update.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showUpdateFoodDialog(adapter.getRef(position).getKey(), adapter.getItem(position));
                    }
                });

                viewHolder.remove.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteFood(adapter.getRef(position).getKey());
                    }
                });
            }

            @Override
            protected void onDataChanged() {
                if(progressBar != null && wait != null){
                    progressBar.setVisibility(View.GONE);
                    wait.setVisibility(View.GONE);
                }
            }
        };

        adapter.notifyDataSetChanged();
        recycler_food.setAdapter(adapter);
    }


    private void deleteFood(final String key){
        AlertDialog.Builder sure = new AlertDialog.Builder(FoodListActivity.this);
        sure.setMessage("Are you sure you want to Delete this Category?");
        sure.setCancelable(false);
        sure.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                foodlist.child(key).removeValue();
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


    private void showUpdateFoodDialog(final String key, final Food item){
        final AlertDialog alertDialog = new AlertDialog.Builder(FoodListActivity.this).create();

        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.add_food_dialog, null);

        closeDialog = view.findViewById(R.id.closeDialog);
        foodName = view.findViewById(R.id.foodName);
        foodDiscount = view.findViewById(R.id.foodDiscount);
        foodID = view.findViewById(R.id.foodID);
        foodPrice = view.findViewById(R.id.foodPrice);
        foodPrePrice = view.findViewById(R.id.foodPrePrice);
        foodDesc = view.findViewById(R.id.foodDesc);
        txtName = view.findViewById(R.id.txtName);
        txtDiscount = view.findViewById(R.id.txtDiscount);
        txtID = view.findViewById(R.id.txtID);
        txtPrice = view.findViewById(R.id.txtPrice);
        txtPrePrice = view.findViewById(R.id.txtPrePrice);
        txtDesc = view.findViewById(R.id.txtDesc);
        selectPicture = view.findViewById(R.id.selectImage);
        uploadImage = view.findViewById(R.id.uploadImage);
        uploadData = view.findViewById(R.id.uploadFood);

        txtName.setText(item.getName());
        txtDiscount.setText(item.getDiscount());
        txtID.setText(item.getMenuId());
        txtPrice.setText(item.getPrice());
        txtPrePrice.setText(item.getPreviousPrice());
        txtDesc.setText(item.getDescription());

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
                chooseImage();
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
                alertDialog.dismiss();

                item.setName(txtName.getText().toString().trim());
                item.setDiscount(txtDiscount.getText().toString().trim());
                item.setMenuId(txtID.getText().toString().trim());
                item.setPrice(txtPrice.getText().toString().trim());
                item.setPreviousPrice(txtPrePrice.getText().toString().trim());
                item.setDescription(txtDesc.getText().toString().trim());
                foodlist.child(key).setValue(item);

                Alerter.create(FoodListActivity.this)
                        .setTitle("MEAL UPDATED!")
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

        alertDialog.show();
    }

    private void changeImage(final Food item) {
        if(imageUri != null)
        {
            final ProgressDialog progressDialog = new ProgressDialog(FoodListActivity.this);
            progressDialog.setMessage("Uploading...");
            progressDialog.show();

            String imageName = UUID.randomUUID().toString();
            final StorageReference imageFolder = storageReference.child("AddedFoods/" + imageName);

            imageFolder.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss();
                            Toast.makeText(FoodListActivity.this, "Upload Successful!", Toast.LENGTH_SHORT).show();

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
                            Toast.makeText(FoodListActivity.this, "Upload Unsuccessful!", Toast.LENGTH_SHORT).show();
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

    public interface FoodClickListener{
        void onClick(View view, int position, boolean isLongClick);
    }

    public static class FoodViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        View view;
        TextView txtFoodName, txtDiscount, txtPrice, txtPrePrice;
        ImageView FoodImage, MealType, Cross;
        Button update, remove;

        private FoodClickListener foodClickListener;

        public FoodViewHolder(@NonNull View itemView) {
            super(itemView);
            view = itemView;

            txtFoodName = view.findViewById(R.id.food_name);
            txtDiscount = view.findViewById(R.id.discount);
            txtPrice = view.findViewById(R.id.present_price);
            txtPrePrice = view.findViewById(R.id.previous_price);

            update = view.findViewById(R.id.updateFood);
            remove = view.findViewById(R.id.deleteFood);

            FoodImage = view.findViewById(R.id.food_img);
            MealType = view.findViewById(R.id.meal_type);
            Cross = view.findViewById(R.id.cross);

            itemView.setOnClickListener(this);
        }

        public void setFoodName(String foodName){
            txtFoodName.setText(foodName);
        }

        public void setFoodImage(Context context, String foodImage){
            Picasso.get().load(foodImage).into(FoodImage);
        }

        public void setMealType(Context mContext, String mealType){
            Picasso.get().load(mealType).into(MealType);
        }

        public void setDiscount(String discount){
            txtDiscount.setText(discount);
        }

        public void setPrice(String price){
            txtPrice.setText(price);
        }

        public void setPreviousPrice(String previousPrice){
            txtPrePrice.setText(previousPrice);
        }

        public void setCross(Context nContext, String cross) {
            Picasso.get().load(cross).into(Cross);
        }

        public void setFoodClickListener(FoodClickListener foodClickListener) {
            this.foodClickListener = foodClickListener;
        }

        @Override
        public void onClick(View v) {
            foodClickListener.onClick(v, getAdapterPosition(), false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu2, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if(id == R.id.menu_search){
            item.setVisible(false);
            materialSearchBar.setVisibility(View.VISIBLE);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
