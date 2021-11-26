package io.github.solrudev.simpleinstaller.sampleapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.solrudev.simpleinstaller.sampleapp.AppData
import io.github.solrudev.simpleinstaller.sampleapp.R
import io.github.solrudev.simpleinstaller.sampleapp.databinding.AppItemBinding

class AppsListAdapter(private val onClick: (AppData) -> Unit) :
	ListAdapter<AppData, AppsListAdapter.AppViewHolder>(AppDataDiffCallback) {

	class AppViewHolder(itemView: View, val onClick: (AppData) -> Unit) : RecyclerView.ViewHolder(itemView) {

		private val binding = AppItemBinding.bind(itemView)
		private var currentAppData: AppData? = null

		init {
			itemView.setOnClickListener {
				currentAppData?.let {
					onClick(it)
				}
			}
		}

		fun bind(data: AppData) {
			currentAppData = data
			binding.appIcon.setImageDrawable(data.icon)
			binding.appName.text = data.name
			binding.appPackageName.text = data.packageName
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(R.layout.app_item, parent, false)
		return AppViewHolder(view, onClick)
	}

	override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
		val appData = getItem(position)
		holder.bind(appData)
	}
}

object AppDataDiffCallback : DiffUtil.ItemCallback<AppData>() {
	override fun areItemsTheSame(oldItem: AppData, newItem: AppData) = oldItem.id == newItem.id
	override fun areContentsTheSame(oldItem: AppData, newItem: AppData) = oldItem == newItem
}