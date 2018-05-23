package com.capstone.smallserver;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.net.URI;
import java.util.ArrayList;


public class CustomListView extends ArrayAdapter<String> {

    private Activity context;
    private ArrayList<String> items;
    private ArrayList<Uri> imgid;
    private ArrayList<String> desc;
    //

    public CustomListView(Activity context, ArrayList<String> items, ArrayList<String> desc, ArrayList<Uri> imgid) {
        super(context, R.layout.listveiw_layout,items);

        this.context=context;
        this.items=items;
        this.desc=desc;
        this.imgid=imgid;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        View r=convertView;
        ViewHolder viewHolder=null;
        if(r==null)
        {
            LayoutInflater layoutInflater=context.getLayoutInflater();
            r=layoutInflater.inflate(R.layout.listveiw_layout,null,true);
            viewHolder=new ViewHolder(r);
            r.setTag(viewHolder);
        }
        else
        {
            viewHolder=(ViewHolder) r.getTag();
        }
        viewHolder.ivw.setImageURI(imgid.get(position));
        viewHolder.tvw1.setText(items.get(position));
        viewHolder.tvw2.setText(desc.get(position));

        return r;

    }

    class ViewHolder
    {
        TextView tvw1;
        TextView tvw2;
        ImageView ivw;
        ViewHolder(View v)
        {
            tvw1=(TextView) v.findViewById(R.id.servername);
            tvw2=(TextView) v.findViewById(R.id.serverdescription);
            ivw= (ImageView) v.findViewById(R.id.imageView);
        }

    }
}
