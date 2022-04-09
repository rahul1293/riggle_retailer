package com.riggle.ui.other.registration

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.recyclerview.widget.LinearLayoutManager
import butterknife.ButterKnife
import butterknife.OnClick
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.riggle.R
import com.riggle.data.firebase.FirebaseConfig
import com.riggle.data.firebase.FirebaseRemoteConfigUtil
import com.riggle.data.firebase.RoleType
import com.riggle.data.firebase.StoreType
import com.riggle.data.location.LocationHandler
import com.riggle.data.location.LocationResultListener
import com.riggle.data.models.APICommonResponse
import com.riggle.data.models.ApiError
import com.riggle.data.models.request.StoreInfo
import com.riggle.data.models.response.RegionsBean
import com.riggle.data.models.response.RetailerDetails
import com.riggle.data.models.response.UserData
import com.riggle.data.models.response.UserDetails
import com.riggle.data.network.ApiResponseListener
import com.riggle.data.permission.PermissionHandler
import com.riggle.data.permission.Permissions
import com.riggle.ui.base.activity.CustomAppCompatActivityViewImpl
import com.riggle.ui.base.connector.CustomAppViewConnector
import com.riggle.ui.dialogs.LoadingDialog
import com.riggle.ui.home.HomeActivity
import com.riggle.ui.other.adapter.SubAreaAdapter
import com.riggle.utils.UserProfileSingleton
import com.riggle.utils.hideKeyboard
import com.riggle.utils.hideKeyboardDialog
import kotlinx.android.synthetic.main.activity_select_delivery_slot.*
import kotlinx.android.synthetic.main.activity_welcome_screen.*
import kotlinx.android.synthetic.main.activity_welcome_screen.etAddress
import kotlinx.android.synthetic.main.activity_welcome_screen.etPinCode
import kotlinx.android.synthetic.main.activity_welcome_screen.rvSubArea
import kotlinx.android.synthetic.main.activity_welcome_screen.tvCity
import kotlinx.android.synthetic.main.activity_welcome_screen.tvState
import org.koin.android.ext.android.inject

class WelcomeScreen : CustomAppCompatActivityViewImpl(), CustomAppViewConnector,
    LocationResultListener {


    private val userPreference: UserProfileSingleton by inject()

    private var firebaseConfig: FirebaseConfig? = null
    private var storeList = arrayListOf<StoreType>()

    private var roleTypeList: List<RoleType>? = null
    private var adapter: StoreTypeAdapter? = null
    private var roleAdapter: RoleTypeAdapter? = null
    private var selectedStoreKey = 0
    private var selectedStoreType = ""
    private var selectedRoleKey: String = ""
    private var mlocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        connectViewToParent(this)
        super.onCreate(savedInstanceState)
    }

    override fun setView(): Int {
        return R.layout.activity_welcome_screen
    }

    override fun initializeViews(savedInstanceState: Bundle?) {
        ButterKnife.bind(this)
        if (firebaseConfig == null) {
            firebaseConfig = FirebaseRemoteConfigUtil.instance?.fireBaseConfigValues
        }
        deactivateBtn()
        setSpinner()
        //setRoleSpinner()


        tvCity.setOnClickListener {
            getRegionList(1)
            rvSubArea?.visibility = View.VISIBLE
        }

        tvState.setOnClickListener {
            getRegionList(2)
            rvSubArea?.visibility = View.VISIBLE
        }

        etStoreName?.onFocusChangeListener = OnFocusChangeListener { view, b ->
            if (!b) {
                if (etStoreName?.text.toString().length > 3) checkValidation() else etStoreName?.error =
                    "Please enter valid store name"
            }
        }
        etPinCode?.onFocusChangeListener = OnFocusChangeListener { view, b ->
            if (!b) if (etPinCode?.text.toString().length == 6) checkValidation() else etPinCode?.error =
                "Please enter valid Pin Code"
        }

        //getRegionList(1)

    }

    private fun getRegionList(type: Int) {
        showHideLoader(true)
        val map = HashMap<String, String>()
        map.put("page", "1")
        map.put("page_size", "20")
        map.put("search", "")
        if (type == 1) {
            map.put("type", "area")
        } else if (type == 2) {
            map.put("belongs__id", area_id.toString())
            map.put("type", "sub_area")
        }
        dataManager.getRegion(object : ApiResponseListener<APICommonResponse<List<RegionsBean>>> {
            override fun onSuccess(response: APICommonResponse<List<RegionsBean>>) {
                showHideLoader(false)
                if (response != null) {
                    response.results?.let {
                        if (it.size > 0) {
                            setSubArea(it, type)
                        }
                    }
                }
            }

            override fun onError(apiError: ApiError?) {
                showHideLoader(false)
                Log.i("TAG", ":::::" + apiError?.message)
            }

        }, map)
    }

    private var area_id = 0
    private var sub_area_id = 0
    private fun setSubArea(results: List<RegionsBean>, type: Int) {
        var datesAdapter = SubAreaAdapter(this, results)
        rvSubArea?.layoutManager =
            LinearLayoutManager(this)
        rvSubArea?.adapter = datesAdapter
        datesAdapter.setListener(object : SubAreaAdapter.DeliveryDateListener {
            override fun dateSelected(bean: RegionsBean) {
                if (type == 1) {
                    area_id = bean.id
                    tvCity?.text = bean.name
                    tvCity.error = null
                    //getRegionList(2)
                } else if (type == 2) {
                    sub_area_id = bean.id
                    tvState?.text = bean.name
                    tvState.error = null
                }
                rvSubArea?.visibility = View.GONE
            }
        })
    }

    private fun setRoleSpinner() {
        roleTypeList = firebaseConfig?.role_type
        roleAdapter = RoleTypeAdapter(this@WelcomeScreen, roleTypeList)
        roleTypeSpinner?.adapter = roleAdapter
        roleTypeSpinner?.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) hideKeyboardDialog(this)
            false
        }
        roleTypeSpinner?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
                val (_, key) = adapterView.getItemAtPosition(i) as RoleType
                selectedRoleKey = key
                checkValidation()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }

    }

    private fun checkValidation() {
        if (/*(selectedRoleKey.isNotEmpty() && selectedRoleKey != "hint") &&*/ selectedStoreKey != 0 && etStoreName?.text.toString().length > 3 && etPinCode?.text?.length == 6) {
            activateBtn()
        } else {
            deactivateBtn()
        }

    }

    private fun setSpinner() {
        //storeList = firebaseConfig?.store_type ?: ArrayList<StoreType>()

        storeList?.apply {
            add(0, StoreType(getString(R.string.store_type), 0))
            add(StoreType("General Store", 1))
            add(StoreType("Dairy", 1))
            add(StoreType("Medical Store", 1))
            add(StoreType("Supermarket", 1))
            add(StoreType("QSR / Food Joint", 1))
            add(StoreType("24 hr convenience store", 1))
            add(StoreType("Other", 1))
        }

        val hint = StoreType(getString(R.string.store_type), 0)
        storeList.add(0, hint)

        adapter = StoreTypeAdapter(this, storeList)
        storeTypeSpinner?.adapter = adapter
        storeTypeSpinner?.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) hideKeyboard(this@WelcomeScreen)
            false
        }
        storeTypeSpinner?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
                val (_, key) = adapterView.getItemAtPosition(i) as StoreType
                selectedStoreKey = key
                selectedStoreType = storeList.get(i).store_type
                checkValidation()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }
    }

    private fun deactivateBtn() {
        btn_submit?.alpha = 0.2f
        btn_submit?.isClickable = false
    }

    private fun activateBtn() {
        btn_submit?.alpha = 1f
        btn_submit?.isClickable = true
    }

    @OnClick(R.id.btn_submit)
    fun submit() {
        /*val storeInfo = StoreInfo()
        storeInfo.pin_code = etPinCode?.text.toString()
        storeInfo.store_name = etStoreName?.text.toString()
        storeInfo.store_type = "" + selectedStoreKey
        storeIn fo.role_type = selectedRoleKey*/
        if (isEmptyField()) {

            val data = HashMap<String, String>()
            data.put("name", etStoreName?.text.toString())
            data.put("pincode", etPinCode?.text.toString())
            data.put("store_type", selectedStoreType)
            data.put("address", etAddress?.text.toString())
            data.put("landmark", etLandMark?.text.toString())
            //data.put("sub_area", sub_area_id.toString())
            try {
                if (cbAtStore.isChecked) {
                    if (mlocation != null) {
                        mlocation?.let {
                            data.put(
                                "store_location",
                                it.latitude.toString() + "," + it.longitude.toString()
                            )//lat,lon
                        }
                    } else {
                        data.put("store_location", ""/*"23.82781,74.484839"*/)//lat,lon
                    }
                } else {
                    data.put("store_location", ""/*"23.82781,74.484839"*/)//lat,lon
                }
            } catch (e: Exception) {
                e.printStackTrace()
                data.put("store_location", ""/*"23.82781,74.484839"*/)//lat,lon
            }
            userPreference.userData?.retailer?.id?.let { id ->
                dataManager.updateRetailerOne(
                    object : ApiResponseListener<JsonElement> {
                        override fun onSuccess(response: JsonElement) {
                            if (response != null) {
                                var retailerDetails =
                                    Gson().fromJson(response.toString(), UserDetails::class.java)
                                var usrData = userPreference.userData
                                /*usrData?.session_key?.let {
                                    response.session_key = it
                                    response.retailer = usrData.retailer
                                }*/
                                usrData?.retailer = retailerDetails
                                userPreference
                                    .updateUserData(usrData)
                                userPreference
                                    .saveRetailerDetails(retailerDetails)
                                HomeActivity.start(applicationContext, false)
                                finish()
                            } else Toast.makeText(this@WelcomeScreen, "", Toast.LENGTH_SHORT)
                                .show()
                        }

                        override fun onError(apiError: ApiError?) {
                            Toast.makeText(
                                this@WelcomeScreen,
                                apiError?.message ?: "Server error, please contact support.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }, id, data
                )
            }
        }

        /*dataManager.editProfile(object : ApiResponseListener<APICommonResponse<UserData>> {
            override fun onSuccess(response: APICommonResponse<UserData>) {
                if (response.isSuccess) {
                    userPreference
                        .updateUserData(response.data)
                    HomeActivity.start(applicationContext)
                    finish()
                } else Toast.makeText(this@WelcomeScreen, "" + response.message, Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onError(apiError: ApiError?) {}
        }, storeInfo)*/
//        HomeActivity.start(applicationContext, false)
//        finish()
    }

    private fun isEmptyField(): Boolean {
        if (TextUtils.isEmpty(etAddress.text.toString().trim())) {
            etAddress.error = "Enter Valid Address"
            return false
        }

        /*if (TextUtils.isEmpty(tvCity.text.toString().trim())) {
            tvCity.error = "Choose Area"
            return false
        }

        if (TextUtils.isEmpty(tvState.text.toString().trim())) {
            tvState.error = "Choose Sub Area"
            return false
        }*/

        if (TextUtils.isEmpty(etLandMark.text.toString().trim())) {
            etLandMark.error = "Enter Valid Landmark"
            return false
        }
        return true
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, WelcomeScreen::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    private var loadingDialog: LoadingDialog? = null
    private fun showHideLoader(state: Boolean) {
        if (loadingDialog != null) {
            if (state) loadingDialog?.show() else loadingDialog?.hide()
        } else {
            loadingDialog = LoadingDialog(activity)
            showHideLoader(state)
        }
    }

    override fun onStart() {
        super.onStart()
        Permissions.check(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
            0,
            object : PermissionHandler() {
                override fun onGranted() {
                    getLocationData()
                }
            })
    }

    private fun getLocationData() {
        var locationHandler = LocationHandler(this@WelcomeScreen, this)
        locationHandler.getUserLocation()
    }

    override fun getLocation(location: Location) {
        this.mlocation = location
    }

}