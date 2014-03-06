package com.uwflow.flow_android.fragment;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.uwflow.flow_android.MainFlowActivity;
import com.uwflow.flow_android.R;
import com.uwflow.flow_android.constant.Constants;
import com.uwflow.flow_android.db_object.ScheduleCourse;
import com.uwflow.flow_android.db_object.ScheduleCourses;
import com.uwflow.flow_android.loaders.UserScheduleLoader;
import com.uwflow.flow_android.network.FlowApiRequestCallbackAdapter;
import com.uwflow.flow_android.network.FlowApiRequests;
import com.uwflow.flow_android.fragment.FullScreenImageFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class ProfileScheduleFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<ScheduleCourse>>, View.OnClickListener {
    private String mProfileID;
    private String mScheduleImageURL;

    private RadioGroup mRadioGroup;
    private ImageView mImageSchedule;
    private Button mBtnExportCal;
    private Button mBtnShare;
    private LinearLayout mScheduleListLayout;
    private LinearLayout mScheduleWeekLayout;
    private Bitmap scheduleBitmap;
    private View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
	    mProfileID = getArguments() != null ? getArguments().getString(Constants.PROFILE_ID_KEY) : null;

        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.profile_schedule_layout, container, false);

        mRadioGroup = (RadioGroup)rootView.findViewById(R.id.radio_group_view);
        mImageSchedule = (ImageView)rootView.findViewById(R.id.image_schedule);
        mImageSchedule.setOnClickListener(this);
        mBtnExportCal = (Button)rootView.findViewById(R.id.btn_export_calendar);
        mBtnShare = (Button)rootView.findViewById(R.id.btn_share);
        mScheduleListLayout = (LinearLayout)rootView.findViewById(R.id.list_layout);
        mScheduleWeekLayout = (LinearLayout)rootView.findViewById(R.id.week_layout);
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radio_list_view:
                        // List layout selected
                        mScheduleListLayout.setVisibility(View.VISIBLE);
                        mScheduleWeekLayout.setVisibility(View.GONE);
                        break;
                    case R.id.radio_week_view:
                        // Week layout selected
                        mScheduleListLayout.setVisibility(View.GONE);
                        mScheduleWeekLayout.setVisibility(View.VISIBLE);
                }
            }
        });

	mBtnShare.setEnabled(false);
        mBtnShare.setOnClickListener(this);
        mBtnExportCal.setOnClickListener(this);

	if (mProfileID == null) {
	    getLoaderManager().initLoader(Constants.LoaderManagerId.PROFILE_SCHEDULE_LOADER_ID, null, this);
	} else {
	    fetchScheduleImage(mProfileID);
	}

        return rootView;

    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_export_calendar:
                // TODO: handle calendar export
                break;
            case R.id.btn_share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, mScheduleImageURL);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out my schedule!");
                startActivity(Intent.createChooser(shareIntent, "Share schedule"));
                break;
            case R.id.image_schedule:
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                Fragment fullScreenImageFragment = new FullScreenImageFragment();

                //TODO:Replace this with original bitmap once the scheduleBitmap is being loaded properly from URL
                Bitmap catImage = BitmapFactory.decodeResource(getActivity().getApplicationContext().getResources(),
                        R.drawable.kitty);
                Bundle bundle = new Bundle();
                bundle.putParcelable("ScheduleImage", catImage);
                fullScreenImageFragment.setArguments(bundle);

                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.content_frame, fullScreenImageFragment);
                transaction.addToBackStack(null);
                transaction.commit();
                break;
        }
    }



    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
    }


    @Override
    public Loader<List<ScheduleCourse>> onCreateLoader(int i, Bundle bundle) {
        return new UserScheduleLoader(getActivity(), ((MainFlowActivity)getActivity()).getHelper());

    }

    @Override
    public void onLoadFinished(Loader<List<ScheduleCourse>> arrayListLoader, final List<ScheduleCourse> scheduleCourses) {
        if (!scheduleCourses.isEmpty() && scheduleCourses.get(0).getScheduleUrl() != null){
            Picasso.with(getActivity().getApplicationContext()).load(scheduleCourses.get(0).getScheduleUrl()).into(new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
                    scheduleBitmap = bitmap;
                    mImageSchedule.setImageBitmap(bitmap);
		            mScheduleImageURL = scheduleCourses.get(0).getScheduleUrl();
		            mBtnShare.setEnabled(true);
                }

                @Override
                public void onBitmapFailed(Drawable drawable) {

                }

                @Override
                public void onPrepareLoad(Drawable drawable) {

                }
            });
        }
    }

    @Override
    public void onLoaderReset(Loader<List<ScheduleCourse>> arrayListLoader) {

    }

    private void fetchScheduleImage(String id){
	if (id == null) return;

	FlowApiRequests.getUserSchedule(
		id,
		new FlowApiRequestCallbackAdapter() {
		    @Override
		    public void getUserScheduleCallback(ScheduleCourses scheduleCourses) {
			if (scheduleCourses.getScreenshotUrl() != null) {
			    // assume the URL is valid and an image will be returned
			    // TODO: change this conditional to 'if the image is successfully fetched'
			    mScheduleImageURL = scheduleCourses.getScreenshotUrl();
			    Picasso.with(getActivity().getApplicationContext())
				    .load(mScheduleImageURL).into(mImageSchedule);
			    mBtnShare.setEnabled(true);
			}
		    }
		});
    }
}


