package com.example.bleoffers

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bleoffers.databinding.BledeviceItemBinding

class LeDeviceListAdapter(
    private val onClickListener: OnClickListener
): RecyclerView.Adapter<LeDeviceListAdapter.ViewHolder>() {

    private var listOfDevices = mutableListOf<ScanResult>()

    inner class ViewHolder(private val view: BledeviceItemBinding): RecyclerView.ViewHolder(view.root) {
        @SuppressLint("MissingPermission")
        fun bind(device: ScanResult) {
            if (device.device.name != null) { view.tvDevice.text =
                    view.root.context.getString(R.string.txt_name, device.device.name) }
            else { view.tvDevice.text =
                view.root.context.getString(R.string.txt_name, "No name") }

            if (device.device.address != null) { view.tvDevice.text = device.device.address }
            else { view.tvAdress.text = "UNKNOWN ADDRESS" }

            if (Build.VERSION.SDK_INT >=30) { view.tvAlias.text = device.device.alias }

            view.tvType.text = device.device.type.toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = BledeviceItemBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listOfDevices[position])
        holder.itemView.setOnClickListener {
            onClickListener.onClick(listOfDevices[position])
        }
    }

    fun devicesList(list: MutableList<ScanResult>){
        listOfDevices = list
        notifyDataSetChanged()
        notifyItemRangeChanged(listOfDevices.indexOf(listOfDevices.first()), itemCount)
    }

    override fun getItemCount(): Int = listOfDevices.size

    class OnClickListener(val clickListener: (device: ScanResult) -> Unit) {
        fun onClick(device: ScanResult) = clickListener(device)
    }
}
