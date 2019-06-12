package com.server.fooddoor;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.server.fooddoor.common.Common;
import com.server.fooddoor.model.Orders;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class OrderDetailsActivity extends AppCompatActivity {

    private TextView orderID, orderPrice, orderMobile, orderAddress;
    private String orderIDValue;

    private RecyclerView order_expanded_list;
    private RecyclerView.LayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        Toolbar toolbar = findViewById(R.id.orderDetails_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
        toolbarTitle.setText(R.string.ForOrderDetailsTitle);
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

        orderID = findViewById(R.id.order_id);
        orderPrice = findViewById(R.id.order_price);
        orderMobile = findViewById(R.id.order_mobile);
        orderAddress = findViewById(R.id.order_address);

        order_expanded_list = findViewById(R.id.order_list);
        order_expanded_list.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        order_expanded_list.setLayoutManager(layoutManager);

        if (getIntent() != null)
        {
            orderIDValue = getIntent().getStringExtra("OrderID");
        }

        orderID.setText(orderIDValue);
        orderPrice.setText(Common.currentRequests.getTotal());
        orderMobile.setText(Common.currentRequests.getPhone());
        orderAddress.setText(Common.currentRequests.getAddress());

        OrderDetailsAdapter orderDetailsAdapter = new OrderDetailsAdapter(Common.currentRequests.getFoods());
        order_expanded_list.setAdapter(orderDetailsAdapter);
        orderDetailsAdapter.notifyDataSetChanged();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder
    {
        View view;
        TextView name, quantity, price, discount;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            view = itemView;

            name = view.findViewById(R.id.product_name);
            quantity = view.findViewById(R.id.product_quantity);
            price = view.findViewById(R.id.product_price);
            discount = view.findViewById(R.id.product_discount);
        }
    }

    public class OrderDetailsAdapter extends RecyclerView.Adapter<MyViewHolder>
    {

        List<Orders> myOrder;

        public OrderDetailsAdapter(List<Orders> myOrder) {
            this.myOrder = myOrder;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View itemView = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.order_details_layout, viewGroup, false);
            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder myViewHolder, int i) {
            Orders orders = myOrder.get(i);
            Locale locale = new Locale("en", "IN");
            NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);
            myViewHolder.name.setText(orders.getFoodName());
            myViewHolder.quantity.setText(orders.getQuantity());
            myViewHolder.price.setText("₹ " + orders.getPrice());
            myViewHolder.discount.setText(orders.getDiscount());
        }

        @Override
        public int getItemCount() {
            return myOrder.size();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
