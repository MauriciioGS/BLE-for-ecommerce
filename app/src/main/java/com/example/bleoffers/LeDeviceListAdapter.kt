package com.example.bleoffers

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bleoffers.databinding.BledeviceItemBinding
import com.example.bleoffers.model.OfferDevice

class LeDeviceListAdapter(
    private val onClickListener: OnClickListener
): RecyclerView.Adapter<LeDeviceListAdapter.ViewHolder>() {

    private var listOfDevices = mutableListOf<OfferDevice>()

    inner class ViewHolder(private val view: BledeviceItemBinding): RecyclerView.ViewHolder(view.root) {
        val tvDevice = view.tvDevice
        val btnOpenOffer = view.btnSeeOffer
            /*view.tvAdress.text = device.address
            view.tvAlias.text = device.alias
            view.tvType.text = device.type.toString()*/
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = BledeviceItemBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvDevice.text = listOfDevices[position].name
        holder.btnOpenOffer.setOnClickListener {
            onClickListener.onClick(listOfDevices[position])
        }
        holder.itemView.setOnClickListener {
            onClickListener.onClick(listOfDevices[position])
        }
    }

    fun devicesList(list: MutableList<OfferDevice>){
        listOfDevices = list
        notifyItemRangeChanged(listOfDevices.indexOf(listOfDevices.first()), itemCount)
    }

    override fun getItemCount(): Int = listOfDevices.size

    class OnClickListener(val clickListener: (device: OfferDevice) -> Unit) {
        fun onClick(device: OfferDevice) = clickListener(device)
    }
}
