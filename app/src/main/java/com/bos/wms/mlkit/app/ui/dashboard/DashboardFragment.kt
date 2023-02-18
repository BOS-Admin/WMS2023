package com.bos.wms.mlkit.app.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bos.wms.mlkit.General
import com.bos.wms.mlkit.app.bosApp.PackingActivity
import com.bos.wms.mlkit.app.bosApp.StockTakeActivity
import com.bos.wms.mlkit.app.bosApp.Transfer.TransferActivity
import com.bos.wms.mlkit.databinding.FragmentDashboardBinding
import com.bos.wms.mlkit.storage.Storage
import kotlinx.android.synthetic.main.fragment_dashboard.view.*


class DashboardFragment : Fragment() {

    private lateinit var dashboardViewModel: DashboardViewModel
    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root


        mStorage= Storage(context?.applicationContext)
        val general=General.getGeneral(context?.applicationContext)
        var FloorID: Int= general.FloorID
        var UserId = general.UserID
        root.textBranch.setText(General.getGeneral(context).fullLocation);
        root.textUser.setText(General.getGeneral(context).userFullName);


       root.btnCount.setOnClickListener{
           general.operationType=100
           general.saveGeneral(context?.applicationContext)
           val intent = Intent (activity, PackingActivity::class.java)
           startActivity(intent)
       }
        root.btnStockTake.setOnClickListener{
            general.operationType=102
            general.saveGeneral(context?.applicationContext)
            val intent = Intent (activity, StockTakeActivity::class.java)
            startActivity(intent)
        }

        root.btnTransfer.setOnClickListener{
            val intent = Intent (activity, TransferActivity::class.java)
            startActivity(intent)
        }

        root.textBranch.isEnabled=false;
        root.textBranch.isEnabled=false;

       //root.layoutDashboard.isVisible=false
       // root.btnTransfer.isEnabled=false

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    lateinit var  mStorage:Storage ;
}