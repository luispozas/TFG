package d2d.testing;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.hardware.Camera;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.io.IOException;
import java.util.ArrayList;

import d2d.testing.gui.StreamDetail;
import d2d.testing.gui.StreamListAdapter;
import d2d.testing.gui.ViewPagerAdapter;
import d2d.testing.net.threads.selectors.RTSPServerSelector;
import d2d.testing.utils.Logger;
import d2d.testing.wifip2p.WifiAwareViewModel;
import d2d.testing.utils.Permissions;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_COARSE_LOCATION_CODE = 101;
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private static final int MY_AUDIO_REQUEST_CODE = 104;
    private static final int MY_WRITE_EXTERNAL_STORAGE_CODE = 103;
    private static final int CHOOSE_FILE_CODE = 102;
    private boolean camera_has_perm = false;
    private boolean audio_has_perm = false;
    private boolean location_has_perm = false;
    private boolean storage_has_perm = false;
    private Camera mCamera;
    private String defaultP2PIp = "192.168.49.1";
    MenuItem wifiItem;

    private WifiAwareViewModel mAwareModel;

    ViewPagerAdapter adapter;

    Permissions wiFiP2pPermissions;

    IntentFilter mIntentFilter;

    private ArrayList<StreamDetail> streamList = new ArrayList();
    private StreamListAdapter arrayAdapter;
    private ListView streamsListView;

    private TextView conectationStatus;
    private TextView myName;
    private TextView myAdd;
    private TextView myStatus;

    private WifiP2pDevice aux;
    private Button record;
    private Button upload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        record = findViewById(R.id.recordButton);
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if(checkCameraHardware()) {
                //wiFiP2pPermissions.camera();
                //wiFiP2pPermissions.audio();
                handleCamera();
                /*
                if(camera_has_perm && audio_has_perm) {
                    //TODO here goes all the functionality
                }
                */
            }
            }
        });

        upload = findViewById(R.id.uploadButton);
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBrowse();
            }
        });

        mAwareModel = new ViewModelProvider(this, new ViewModelProvider.AndroidViewModelFactory(getApplication())).get(WifiAwareViewModel.class);
        initialWork();
    }

    public void set_camera_has_perm(boolean camera){
        this.camera_has_perm = camera;
    }
    public void set_audio_has_perm(boolean audio){
        this.audio_has_perm = audio;
    }
    public void set_location_has_perm(boolean location){
        this.location_has_perm = location;
    }
    public void set_storage_has_perm(boolean storage){
        this.storage_has_perm = storage;
    }

    public Permissions getWiFiP2pPermissions() {
        return wiFiP2pPermissions;
    }
    public boolean get_storage_has_perm(){
        return storage_has_perm;
    }


    private void initialWork() {

        streamsListView = findViewById(R.id.streamListView);
        arrayAdapter = new StreamListAdapter(getApplicationContext(), streamList);
        streamsListView.setAdapter(arrayAdapter);
        execListener();

        conectationStatus = findViewById(R.id.connectionStatus);
        myAdd = findViewById(R.id.my_address);
        myName = findViewById(R.id.my_name);
        myStatus = findViewById(R.id.my_status);
        conectationStatus.setVisibility(TextView.INVISIBLE); // this text was used for debug, maybe u want to activate it again
        //updateMyDeviceStatus(...);

        try {
            if(mAwareModel.createSession()){
                if(mAwareModel.publishService("Server", MainActivity.this)){
                    Toast.makeText(MainActivity.this, "Se creo una sesion de publisher con WifiAware", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(MainActivity.this, "No se pudo crear una sesion de publisher de WifiAware", Toast.LENGTH_SHORT).show();
                }
                if(mAwareModel.subscribeToService("Server", this)){
                    Toast.makeText(MainActivity.this, "Se creo una sesion de subscripcion con WifiAware", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(MainActivity.this, "No se pudo crear una sesion de subscripcion de WifiAware", Toast.LENGTH_SHORT).show();
                }
            }else {
                Toast.makeText(MainActivity.this, "No se pudo crear la sesion de WifiAware", Toast.LENGTH_SHORT).show();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void DiscoverPeers(){

    }

    public void updateStreamsCounter(int count){
        adapter.setStreamNumber(count);
        adapter.notifyDataSetChanged();
    }

    private void handleCamera(){
        //this.mCamera = getCameraInstance();
        //openCameraActivity();
        openStreamActivity();
    }

    /*
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_items, menu);
        wifiItem = menu.getItem(0);
        //updateWifiIcon(WifiP2pController.getInstance().isWifiEnabled());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.atn_direct_enable:

                return true;

            case R.id.atn_direct_discover:
                wiFiP2pPermissions.location();
                if(location_has_perm) {
                    DiscoverPeers();
                }
                return true;


            case R.id.recordButton:
                if(checkCameraHardware()) {
                    //wiFiP2pPermissions.camera();
                    //wiFiP2pPermissions.audio();
                    handleCamera();
                    /*
                    if(camera_has_perm && audio_has_perm) {
                        //TODO here goes all the functionality

                    }

                }
                return true;
            /*case R.id.atn_direct_file_transfer:
                    onBrowse();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
    */

    public static String getDeviceStatus(int deviceStatus) {
        //Log.d(MainActivity.TAG, "Peer status :" + deviceStatus);
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";

        }
    }

    @SuppressLint("NewApi")
    public void updateWifiIcon(boolean wifi) {
        if(wifiItem!= null) {
            if(wifi) {
                wifiItem.setIconTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_green_light)));
            } else {
                wifiItem.setIconTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_red_dark)));
            }
        }
    }

    public void updateStreamList(final boolean on_off,final String ip, final String name){
        runOnUiThread(new Runnable() {
            public void run() { updateList(on_off, ip, name);
            }
        });
    }

    public void onBrowse() {
        Intent chooseFile;
        Intent intent;
        chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
        chooseFile.setType("image/*");
        intent = Intent.createChooser(chooseFile, "Choose a file");
        startActivityForResult(intent, CHOOSE_FILE_CODE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        if (requestCode == CHOOSE_FILE_CODE) {
            Uri uri = data.getData();
            Logger.d("Path selected " + uri);

        }
    }

    @Override
    public void onResume() {
        super.onResume();

//        editText.clearFocus();
    }
    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            RTSPServerSelector.getInstance().stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_COARSE_LOCATION_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.location_has_perm = true;
                    // permission was granted,
                    Toast.makeText(getApplicationContext(), "LOCATION PERMISSION GRANTED", Toast.LENGTH_SHORT).show();
                    DiscoverPeers();
                } else {

                    // permission denied, wifi direct wont work under version ??? maybe we dont need it....

                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        //Show permission explanation dialog...
                        Toast.makeText(getApplicationContext(), "YOU DENIED PERMISSION AND CHECKED TO NEVER ASK AGAIN, GO SETTING AND ADD LOCATION PERMISSION MANUALLY TO USE THIS", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Toast.makeText(getApplicationContext(), "LOCATION PERMISSION NOT GRANTED, YOU WONT BE ABLE TO USE THIS", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            }
            case MY_CAMERA_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.camera_has_perm = true;
                    // permission was granted,
                    Toast.makeText(getApplicationContext(), "CAMERA PERMISSION GRANTED", Toast.LENGTH_SHORT).show();
                    if(audio_has_perm) {
                        handleCamera();
                    }
                    else {
                        wiFiP2pPermissions.audio();
                    }
                } else {

                    // permission denied, wifi direct wont work under version ??? maybe we dont need it...
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                        //Show permission explanation dialog...
                        Toast.makeText(getApplicationContext(), "YOU DENIED PERMISSION AND CHECKED TO NEVER ASK AGAIN, GO SETTING AND ADD CAMERA PERMISSION MANUALLY TO USE THIS", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Toast.makeText(getApplicationContext(), "CAMERA PERMISSION NOT GRANTED, YOU WONT BE ABLE TO USE THIS", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            }
            case MY_AUDIO_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.audio_has_perm = true;
                    // permission was granted,
                    Toast.makeText(getApplicationContext(), "AUDIO RECORD PERMISSION GRANTED", Toast.LENGTH_SHORT).show();
                    if(camera_has_perm) {
                        handleCamera();
                    }
                    else {
                        wiFiP2pPermissions.camera();
                    }
                } else {

                    // permission denied, wifi direct wont work under version ??? maybe we dont need it...
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                        //Show permission explanation dialog...
                        Toast.makeText(getApplicationContext(), "YOU DENIED PERMISSION AND CHECKED TO NEVER ASK AGAIN, GO SETTING AND ADD AUDIO PERMISSION MANUALLY TO USE THIS", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Toast.makeText(getApplicationContext(), "AUDIO RECORD PERMISSION NOT GRANTED, YOU WONT BE ABLE TO USE THIS", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            }
            case MY_WRITE_EXTERNAL_STORAGE_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.storage_has_perm = true;
                    // permission was granted,
                    Toast.makeText(getApplicationContext(), "STORAGE PERMISSION GRANTED", Toast.LENGTH_SHORT).show();
                } else {

                    // permission denied, wifi direct wont work under version ??? maybe we dont need it...
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        //Show permission explanation dialog...
                        Toast.makeText(getApplicationContext(), "YOU DENIED PERMISSION AND CHECKED TO NEVER ASK AGAIN, GO SETTING AND ADD STORAGE PERMISSION MANUALLY TO USE THIS", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Toast.makeText(getApplicationContext(), "STORAGE PERMISSION NOT GRANTED, YOU WONT BE ABLE TO USE THIS,TRY AGAIN", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            }
        }
    }

    private boolean checkCameraHardware() {
        if (this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            //Toast.makeText(getApplicationContext(), "YOUR DEVICE HAS CAMERA", Toast.LENGTH_SHORT).show();
            return true;
        } else {
            // no camera on this device
            Toast.makeText(getApplicationContext(), "YOUR DEVICE HAS NO CAMERA", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    public ArrayList<StreamDetail> getStreamlist(){
        return getStreamList();
    }

    private void openCameraActivity() {
        Intent cameraActivityIntent = new Intent(this, CameraActivity.class);
        this.startActivity(cameraActivityIntent);
    }

    private void openStreamActivity() {
        Intent streamActivityIntent = new Intent(this, StreamActivity.class);
        this.startActivity(streamActivityIntent);
    }


    public void openViewStreamActivity(String ip) {
        Intent streamActivityIntent = new Intent(this, ViewStreamActivity.class);
        streamActivityIntent.putExtra("IP",ip);
        this.startActivity(streamActivityIntent);
    }

    public void setDefaultP2PIp(final String ip){
        runOnUiThread(new Runnable() {
            public void run() {
                defaultP2PIp = ip;
            }
        });
    }

    //---------------------------------------------------------------------------------------------------------------------------- LISTA DE STREAMING
    public void updateList(boolean on_off, String ip, String name){
        if(!ip.equals("0.0.0.0")) {
            StreamDetail detail = new StreamDetail(name,ip);
            if (on_off) {
                if (!streamList.contains(detail))
                    streamList.add(detail);
            } else {
                if (streamList.contains(detail))
                    streamList.remove(detail);
            }

            arrayAdapter.notifyDataSetChanged();
            updateStreamsCounter(streamList.size()); //update TAB title
        }
    }
    private void execListener() {

        streamsListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openViewStreamActivity(streamList.get(position).getIp());
            }
        });

    }

    public ArrayList<StreamDetail> getStreamList(){
        return this.streamList;
    }

    //----------------------------------------------------------------------------------------------------------------------------- Informacion de dispositivo
    public void setConectationStatus(String status){
        conectationStatus.setText(status);
    }

    public void updateMyDeviceStatus (WifiP2pDevice device) {
        if(myName != null && myStatus != null && myAdd != null) {
            myName.setText(device.deviceName);
            myStatus.setText(getDeviceStatus(device.status));
            myAdd.setText(device.deviceAddress);
        }
        else {
            aux = device;
            UpdateMyInfo task = new UpdateMyInfo();
            task.execute();
        }
    }

    //esto quiza se pueda arreglar de otra manera,esto se hace porque
    //se intenta hacer el updateMyDeviceStatus y a veces se lanza una excepcion porque
    //los textViews aun no se han inicializado
    private class UpdateMyInfo extends AsyncTask<Void, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            while(myName == null && myStatus == null && myAdd == null){

            }

            return true;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(Boolean result) {
            myName.setText(aux.deviceName);
            myStatus.setText(getDeviceStatus(aux.status));
            myAdd.setText(aux.deviceAddress);
        }

        @Override
        protected void onCancelled() {
        }
    }
}
