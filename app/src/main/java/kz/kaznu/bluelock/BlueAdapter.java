package kz.kaznu.bluelock;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by c0rp on 2/14/17.
 */

public class BlueAdapter extends RecyclerView.Adapter<BlueAdapter.ViewHolder> {


    private Context mContext;
    private List<BluePayDevice> devices;

    public static class ViewHolder  extends RecyclerView.ViewHolder{
        TextView name;
        TextView rssi;

        public ViewHolder(View itemView) {
            super(itemView);
            this.name = (TextView) itemView.findViewById(R.id.device_name);
            this.rssi = (TextView) itemView.findViewById(R.id.device_rssi);
        }
    }


    public BlueAdapter(Context mContext, List<BluePayDevice> devices) {
        this.mContext = mContext;
        this.devices = devices;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_listview, parent, false);
        // set the view's size, margins, paddings and layout parameters

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final BluePayDevice bluePayDevice = devices.get(position);
        holder.name.setText(bluePayDevice.getName() + bluePayDevice.getUniqueCode());
        holder.rssi.setText(String.valueOf(bluePayDevice.getRssi()));
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

}
