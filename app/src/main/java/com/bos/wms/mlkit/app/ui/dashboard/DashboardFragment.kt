package com.bos.wms.mlkit.app.ui.dashboard

import Remote.APIClient
import Remote.BasicApi
import Remote.UserPermissions.UserPermissions
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bos.wms.mlkit.General
import com.bos.wms.mlkit.app.ZebraPrinter
import com.bos.wms.mlkit.app.bosApp.PackingActivity
import com.bos.wms.mlkit.app.bosApp.ScanRackPutAwayActivity
import com.bos.wms.mlkit.app.bosApp.StockTakeActivity
import com.bos.wms.mlkit.app.bosApp.Transfer.TransferActivity
import com.bos.wms.mlkit.databinding.FragmentDashboardBinding
import com.bos.wms.mlkit.storage.Storage
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_dashboard.view.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import retrofit2.HttpException
import java.io.IOException


class DashboardFragment : Fragment() {

    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var general: General
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


        mStorage = Storage(context?.applicationContext)
        general = General.getGeneral(context?.applicationContext)
        var FloorID: Int = general.FloorID
        var UserId = general.UserID
        root.textBranch.setText(General.getGeneral(context).fullLocation);
        root.textUser.setText(General.getGeneral(context).userFullName);

        if(UserPermissions.PermissionsReceived()){
            UserPermissions.ValidatePermission("WMSApp.Dashboard.Packing", root.btnCount);
            UserPermissions.ValidatePermission("WMSApp.Dashboard.StockTake", root.btnStockTake);
            UserPermissions.ValidatePermission("WMSApp.Dashboard.Transfer", root.btnTransfer);
//            UserPermissions.ValidatePermission("WMSApp.Dashboard.PutAway", root.btnPutAway);

        }else {
            UserPermissions.AddOnReceiveListener {
                UserPermissions.ValidatePermission("WMSApp.Dashboard.Packing", root.btnCount);
                UserPermissions.ValidatePermission("WMSApp.Dashboard.StockTake", root.btnStockTake);
                UserPermissions.ValidatePermission("WMSApp.Dashboard.Transfer", root.btnTransfer);
//                UserPermissions.ValidatePermission("WMSApp.Dashboard.PutAway", root.btnPutAway);
            }
        }

        root.btnCount.setOnClickListener {
            general.operationType = 100
            general.saveGeneral(context?.applicationContext)
            val intent = Intent(activity, PackingActivity::class.java)
            startActivity(intent)
        }
        root.btnStockTake.setOnClickListener {
            general.operationType = 102
            general.saveGeneral(context?.applicationContext)
            val intent = Intent(activity, StockTakeActivity::class.java)
            startActivity(intent)
        }

        root.btnTransfer.setOnClickListener {
            val intent = Intent(activity, TransferActivity::class.java)
            startActivity(intent)
        }
//        root.btnPutAway.setOnClickListener {
//            val intent = Intent(activity, ScanRackPutAwayActivity::class.java)
//            startActivity(intent)
//        }

        root.textBranch.isEnabled = false;
        root.textBranch.isEnabled = false;

        //root.layoutDashboard.isVisible=false
        // root.btnTransfer.isEnabled=false

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    lateinit var mStorage: Storage;
    lateinit var api: BasicApi
    var compositeDisposable = CompositeDisposable()

}