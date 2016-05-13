package net.goeasyway.bundle1;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import net.goeasyway.easyand.bundlemananger.BundleManager;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String mapPackageName = "com.amap.map3d.demo";
        String viewClassName = "com.amap.api.maps.MapView";

        LinearLayout layout = (LinearLayout)findViewById(R.id.view);
        View mapView = BundleManager.getInstance().getBundleView(this, mapPackageName, viewClassName);
        if (mapView != null) {
            layout.addView(mapView);
        }
    }
}
