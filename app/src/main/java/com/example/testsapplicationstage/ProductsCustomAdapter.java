package com.example.testsapplicationstage;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.testsapplicationstage.objects.Product;

import java.util.List;

public class ProductsCustomAdapter extends RecyclerView.Adapter<ProductsCustomAdapter.ViewHolder> {

    private final List<Product> localDataSet;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        public ViewHolder(View view) {
            super(view);
            textView = view.findViewById(R.id.textView);

            view.setOnClickListener(view1 -> {
                // TODO Ajouter a la commande courante.
            });
        }

        public TextView getTextView() {
            return textView;
        }
    }

    public ProductsCustomAdapter(List<Product> dataSet) {
        localDataSet = dataSet;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.text_row_item, viewGroup, false);
        return new ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.getTextView().setText(localDataSet.get(position).toString());
        if (localDataSet.get(position).getStock_reel() != 0)
            viewHolder.getTextView().setTextColor(Color.parseColor("#888888"));
        else
            viewHolder.getTextView().setTextColor(Color.parseColor("#FF0000"));

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return localDataSet.size();
    }
}
