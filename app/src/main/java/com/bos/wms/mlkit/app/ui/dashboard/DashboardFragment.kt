package com.bos.wms.mlkit.app.ui.dashboard

import Remote.APIClient
import Remote.BasicApi
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
import com.bos.wms.mlkit.app.bosApp.PackingActivity
import com.bos.wms.mlkit.app.bosApp.ScanRackPutAwayActivity
import com.bos.wms.mlkit.app.bosApp.StockTakeActivity
import com.bos.wms.mlkit.app.bosApp.Transfer.TransferActivity
import com.bos.wms.mlkit.databinding.FragmentDashboardBinding
import com.bos.wms.mlkit.storage.Storage
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_dashboard.view.*
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
            try {
                root.btnTransfer.isEnabled = false
                api = APIClient.getInstance(general.ipAddress, false).create(BasicApi::class.java)
                compositeDisposable.addAll(
                    api.GetUserRole(general.UserID)
                        .subscribeOn(Schedulers.io())
                        // .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { s ->
                                var response = try {
                                    s.string()
                                } catch (e: IOException) {
                                    e.message.toString()
                                }

                                if (response != null && response.lowercase().contains("transfer")) {
                                    activity?.runOnUiThread {
                                        root.btnTransfer.isEnabled = true
                                        val intent = Intent(activity, TransferActivity::class.java)
                                        startActivity(intent)
                                    }
                                } else {
                                    activity?.runOnUiThread {
                                        root.btnTransfer.isEnabled = true
                                        Toast.makeText(
                                            activity?.applicationContext,
                                            "Not Allowed\nUser Role Type($response)",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    Log.i("Checking", response)


                                }

                            },
                            { t: Throwable? ->
                                run {

                                    var msg = "Error"
                                    if (t is HttpException) {
                                        var ex: HttpException = t as HttpException

                                        msg = ex.response().errorBody()!!
                                            .string() + " (API Http Error) "


                                    } else {
                                        msg = t?.message + " (API Error)"
                                    }
                                    activity?.runOnUiThread {
                                        root.btnTransfer.isEnabled = true
                                        Toast.makeText(
                                            activity?.applicationContext,
                                            msg, Toast.LENGTH_LONG
                                        ).show()
                                    }

                                }
                            }
                        )
                )
            } catch (e: Throwable) {
                activity?.runOnUiThread {
                    root.btnTransfer.isEnabled = true
                    Toast.makeText(
                        activity?.applicationContext,
                        e.message, Toast.LENGTH_LONG
                    ).show()
                }

            } finally {


            }


        }
        root.btnPutAway.setOnClickListener {
            val intent = Intent(activity, ScanRackPutAwayActivity::class.java)
            startActivity(intent)
        }

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