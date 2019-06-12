package com.server.fooddoor;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jaredrummler.materialspinner.MaterialSpinner;
import com.server.fooddoor.common.Common;
import com.server.fooddoor.model.MyResponse;
import com.server.fooddoor.model.Notification;
import com.server.fooddoor.model.OrderRequests;
import com.server.fooddoor.model.Sender;
import com.server.fooddoor.model.Token;
import com.server.fooddoor.remote.APIService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderStatus extends AppCompatActivity {

    RecyclerView recyclerOrder;
    RecyclerView.LayoutManager layoutManager;

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseDatabase database;
    private DatabaseReference request;

    APIService mService;

    ImageView close;
    MaterialSpinner spinner;
    Button updateStatus;

    FirebaseRecyclerAdapter<OrderRequests, OrderViewHolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_status);

        Toolbar toolbar = findViewById(R.id.order_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
        toolbarTitle.setText(R.string.ForOrdersTitle);
        toolbar.setNavigationIcon(R.drawable.close);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        database = FirebaseDatabase.getInstance();
        request = database.getReference("Order Requests");

        mService = Common.getFCMClient();

        recyclerOrder = findViewById(R.id.recycler_orders);
        recyclerOrder.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerOrder.setLayoutManager(layoutManager);

        loadOrdersList();      //load all orders
    }

    private void loadOrdersList() {
        adapter = new FirebaseRecyclerAdapter<OrderRequests, OrderViewHolder>(
                OrderRequests.class,
                R.layout.order_cards,
                OrderViewHolder.class,
                request
        ) {
            @Override
            protected void populateViewHolder(OrderViewHolder viewHolder, final OrderRequests model, final int position) {
                viewHolder.setOrderID(adapter.getRef(position).getKey());
                viewHolder.setOrderStatus(convertCodeToStatus(model.getStatus()));
                viewHolder.setOrderPrice(model.getTotal());
                viewHolder.setOrderMobile(model.getPhone());
                viewHolder.setOrderAddress(model.getAddress());

                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        //implement it just to fix crash on click
                    }
                });

                viewHolder.details.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent orderDetails = new Intent(OrderStatus.this, OrderDetailsActivity.class);
                        Common.currentRequests = model;
                        orderDetails.putExtra("OrderID", adapter.getRef(position).getKey());
                        startActivity(orderDetails);
                    }
                });

                viewHolder.update.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showOrderUpdateDialog(adapter.getRef(position).getKey(), adapter.getItem(position));
                    }
                });

                viewHolder.delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteOrder(adapter.getRef(position).getKey());
                    }
                });

                viewHolder.track.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent trackOrder = new Intent(OrderStatus.this, TrackOrderActivity.class);
                        Common.currentRequests = model;
                        startActivity(trackOrder);
                    }
                });
            }
        };

        adapter.notifyDataSetChanged();
        recyclerOrder.setAdapter(adapter);
    }

    private String convertCodeToStatus(String status) {
        if (status.equals("0")) {
            return "PLACED";
        } else if (status.equals("1")) {
            return "COOKING";
        } else if (status.equals("2")) {
            return "ON THE WAY";
        }
        else {
            return "DELIVERED";
        }
    }

    private void showOrderUpdateDialog(String key, final OrderRequests item) {
        final AlertDialog alertDialog = new AlertDialog.Builder(OrderStatus.this).create();

        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.order_update_dialog, null);

        close = view.findViewById(R.id.closeDialog);
        spinner = view.findViewById(R.id.orderStatusSpinner);
        spinner.setItems("PLACED" , "COOKING", "ON THE WAY", "DELIVERED");
        updateStatus = view.findViewById(R.id.updateStatus);

        alertDialog.setView(view);

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        final String localKey = key;
        updateStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                item.setStatus(String.valueOf(spinner.getSelectedIndex()));

                request.child(localKey).setValue(item);

                sendOrderStatusToUser(localKey, item);
            }
        });

        alertDialog.show();
    }

    private void sendOrderStatusToUser(final String key, final OrderRequests item) {
        DatabaseReference tokens = database.getReference("Tokens");
        tokens.orderByKey().equalTo(item.getPhone())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot postSnapshot : dataSnapshot.getChildren())
                        {
                            Token token = postSnapshot.getValue(Token.class);
                            Notification notification = new Notification("Food@Door", "Your Food@Door Order having Order No. " + key + "is " + item.getStatus());
                            Sender content = new Sender(token.getToken(), notification);

                            mService.sendNotification(content)
                                    .enqueue(new Callback<MyResponse>() {
                                        @Override
                                        public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                                            if(response.body().success == 1)
                                            {
                                                Toast.makeText(OrderStatus.this, "An Order Status was updated!", Toast.LENGTH_SHORT).show();
                                            }
                                            else
                                            {
                                                Toast.makeText(OrderStatus.this, "An Order Status was updated but unable to send Notification!", Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<MyResponse> call, Throwable t) {
                                            Log.e("ERROR", t.getMessage());
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void deleteOrder(String key) {
        request.child(key).removeValue();
    }

    public interface ItemClickListener{
        void onClick(View view, int position, boolean isLongClick);
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        View view;
        TextView orderID, orderStatus, orderPrice, orderMobile, orderAddress;
        ImageView details, update, delete, track;
        private ItemClickListener itemClickListener;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            view = itemView;

            details = view.findViewById(R.id.orderDetails);
            update = view.findViewById(R.id.orderUpdate);
            delete = view.findViewById(R.id.orderDelete);
            track = view.findViewById(R.id.orderTrack);
            orderID = view.findViewById(R.id.orderID);
            orderStatus = view.findViewById(R.id.orderStatus);
            orderPrice = view.findViewById(R.id.order_price);
            orderMobile = view.findViewById(R.id.order_mobile);
            orderAddress = view.findViewById(R.id.order_address);
        }

        public void setOrderID(String ID){
            orderID.setText(ID);
        }

        public void setOrderStatus(String status){
            orderStatus.setText(status);
        }

        public void setOrderPrice(String price){
            orderPrice.setText(price);
        }

        public void setOrderMobile(String mobile){
            orderMobile.setText(mobile);
        }

        public void setOrderAddress(String address){
            orderAddress.setText(address);
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
    public void onBackPressed() {
        super.onBackPressed();
    }
}
