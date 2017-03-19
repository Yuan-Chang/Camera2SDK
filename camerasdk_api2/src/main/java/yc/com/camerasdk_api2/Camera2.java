package yc.com.camerasdk_api2;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

/**
 * Created by ychang3 on 2/26/17.
 */

public class Camera2 {

    final static int layoutID = 56755;

    public static Camera2Fragment camera2Fragment;

    public static Camera2Fragment init(AppCompatActivity activity)
    {
        ViewGroup viewGroup = (ViewGroup) activity.findViewById(android.R.id.content);
        if (viewGroup == null)
            return null;

        RelativeLayout parentLayout = new RelativeLayout(activity);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.MATCH_PARENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        parentLayout.setLayoutParams(params);
        parentLayout.setId(layoutID);
        //parentLayout.setBackgroundColor(activity.getResources().getColor(android.R.color.holo_green_light));

        if (camera2Fragment == null)
        {
            camera2Fragment = new Camera2Fragment();
        }
        FragmentManager fm = activity.getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        ft.add(layoutID, camera2Fragment);
        ft.commit();

        viewGroup.addView(parentLayout);

        return camera2Fragment;
    }


}
