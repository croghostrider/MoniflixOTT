package com.example.myottapp.UI;

import android.app.Fragment;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseFragment;
import androidx.leanback.app.RowsFragment;
import androidx.leanback.app.RowsSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.BrowseFrameLayout;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;

import android.provider.ContactsContract;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.myottapp.R;
import com.example.myottapp.Service.ApplicationController;
import com.example.myottapp.Service.CustomRequest;
import com.example.myottapp.Service.VolleyRequest;
import com.example.myottapp.VolleyCallback;
import com.example.myottapp.extras.Processor;
import com.example.myottapp.models.AllCategoriesList;
import com.example.myottapp.models.AllLanguagesList;
import com.example.myottapp.models.Category;
import com.example.myottapp.models.DataModel;
import com.example.myottapp.models.Language;
import com.example.myottapp.models.Movie;
import com.example.myottapp.models.MovieBasicInfo;
import com.example.myottapp.models.MovieBasicInfoList;
import com.example.myottapp.models.MovieList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;

public class MainFragment extends RowsFragment {
    private static final String TAG = "MainFragment";

    //super.onActivityCreated(savedInstanceState);
//initializeModels();

    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private static int response_error_count=0;
    private static int NUM_ROWS = 5;
    private static int NUM_COLS = 15;

    static int count=0;
    public ArrayObjectAdapter rowsAdapter;

    private static String[] languages;
    private ProgressBar progressBar;
    private final Handler mHandler = new Handler();
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer;
    private String mBackgroundUri;
    private BackgroundManager mBackgroundManager;
    public static AllCategoriesList allCategoriesList;
    public static AllLanguagesList allLanguagesList;
    public static com.example.myottapp.models.MovieList moviesList;
    public  static List<Language> staticLanguageList=new ArrayList<Language>();
    public static boolean responseFlagCategories=false;
    public static boolean responseFlagMovies=false;


    public void getLanguagesList(){
        String []languageNames={"Kannada","Telugu","Malayalum","Tamil","Hindi","English","Korean","Russian"};
        if(staticLanguageList.size()==0) {
            for (int i = 0; i < languageNames.length; i++) {
                staticLanguageList.add(new Language(i, languageNames[i]));
            }
        }
    }

    private void loadLanguages() {
        LanguageCardPresenter languageCardPresenter=new LanguageCardPresenter();
        ArrayObjectAdapter languagesRowAdapter = new ArrayObjectAdapter(languageCardPresenter);
        //for(int n=0;n<staticLanguageList.size();n++){
        //    languagesRowAdapter.add(staticLanguageList.get(n));
        //}
        languagesRowAdapter.addAll(0,staticLanguageList);
        HeaderItem headerItem=new HeaderItem(0,"Languages");
        rowsAdapter.add(new ListRow(headerItem, languagesRowAdapter));
        setAdapter(rowsAdapter);
    }

    public void getCategories(){
        VolleyRequest volleyRequest=new VolleyRequest();
        volleyRequest.sendGetRequest(new VolleyCallback() {
            @Override
            public void onSuccess() {
                AllCategoriesList allCategoriesList=AllCategoriesList.parseJSON("{categories:"+volleyRequest.getResponseString()+"}");
                DataModel.CategoriesList=allCategoriesList.getCategories();
                System.out.println(DataModel.CategoriesList);
                loadMovieRows();
            }
        },DataModel.movieCategoriesURL,"");
    }

    public void getMovies(String categoryName, int filterValue, int pageNo, int pageSize){
        String tag=categoryName;
        VolleyRequest volleyRequest=new VolleyRequest();
        JSONObject params= volleyRequest.paramsObjectBuilder(1,3,filterValue,pageNo,pageSize);
        volleyRequest.sendPostRequest(new VolleyCallback() {
            @Override
            public void onSuccess() {
                MovieBasicInfoList movieBasicInfoList=MovieBasicInfoList.parseJSON("{movieBasicInfos:"+volleyRequest.getResponseString()+"}");
                List<MovieBasicInfo> list = movieBasicInfoList.getMovieBasicInfos();
                createRow(filterValue,categoryName,list);
            }
        },DataModel.movieByfilterURL,params,tag);
    }


    public void createRow(int categoryID, String categoryName, List<MovieBasicInfo> list){
        CardPresenter cardPresenter=new CardPresenter();
        ArrayObjectAdapter categoryRowAdapter=new ArrayObjectAdapter(cardPresenter);
        //for(int i=0;i<list.size();i++)
            //categoryRowAdapter.add(list.get(i));
        categoryRowAdapter.addAll(0,list);
        HeaderItem headerItem=new HeaderItem(categoryID,categoryName);
        if(list.size()!=0) {
            rowsAdapter.add(new ListRow(headerItem, categoryRowAdapter));
            setAdapter(rowsAdapter);
        }
    }


    public void loadMovieRows(){
        for(Category c: DataModel.CategoriesList){
            getMovies(c.getName(),c.getId(),0,10);
        }
    }

    public void loadUIElements(){
        getLanguagesList();
        getCategories();  //loads movie rows on success of retrieving categories.
        loadLanguages();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter(FocusHighlight.ZOOM_FACTOR_LARGE));
        setAdapter(rowsAdapter);
        loadUIElements();
        setupEventListeners();
        }


    @Override
    public void onResume() {
        super.onResume();
        mBackgroundManager.setDrawable(mDefaultBackground);
    }

    @Override
    public void setAlignment(int windowAlignOffsetFromTop) {
        super.setAlignment(100);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prepareBackgroundManager();
    }

    private void prepareBackgroundManager () {
        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        Fragment fragment = (Fragment) getFragmentManager().findFragmentById(R.id.main_browse_fragment);
        fragment.getView().setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.transparent_to_dark_reverse_shade));
        mBackgroundManager.setDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.default_background));
    }

    private void setupEventListeners() {
        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof MovieBasicInfo) {
                String tag=((MovieBasicInfo) item).getId()+"";
                Intent intent = new Intent(getActivity(), DetailsActivityNew.class);
                intent.putExtra(DetailsActivityNew.MOVIE, ((MovieBasicInfo)item));
                getActivity().startActivity(intent);
                /*VolleyRequest volleyRequest=new VolleyRequest();
                volleyRequest.sendJSONObjGetRequest(new VolleyCallback() {
                    @Override
                    public void onSuccess() {
                        Gson gson=new GsonBuilder().create();
                        Movie movie = gson.fromJson(volleyRequest.getResponseString(),Movie.class);
                        Intent intent = new Intent(getActivity(), DetailsActivityNew.class);
                        intent.putExtra(DetailsActivityNew.MOVIE, movie);
                        getActivity().startActivity(intent);
                    }
                },DataModel.movieDetailsByIdURL+((MovieBasicInfo) item).getId(),tag);
                */
            }
            else if(item instanceof Language){
                Intent intent=new Intent(getActivity(),LanguageActivity.class);
                intent.putExtra(LanguageActivity.LANGUAGE,((Language)item));
                getActivity().startActivity(intent);
            }
            else if (item instanceof String) {
                if (((String) item).contains(getString(R.string.error_fragment))) {
                    Intent intent = new Intent(getActivity(), BrowseErrorActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(
                Presenter.ViewHolder itemViewHolder,
                Object item,
                RowPresenter.ViewHolder rowViewHolder,
                Row row) {
            if(rowsAdapter.indexOf(row)==0){
                ((MainActivity)getActivity()).showCarousal();
                ((MainActivity)getActivity()).hideMovieDetails();
            }
            if(rowsAdapter.indexOf(row)==1){
                ((MainActivity)getActivity()).hideCarousal();
                ((MainActivity)getActivity()).showMovieDetails();
            }
            if (item instanceof MovieBasicInfo) {
                ((MainActivity)getActivity()).setMovieName(((MovieBasicInfo) item).getTitle());
                //((MainActivity)getActivity()).setMovieLanguage(((Movie) item).getLanguageName());
                //((MainActivity)getActivity()).setMovieDescription(((Movie) item).getDescription());
                //((MainActivity)getActivity()).setMovieRuntime(((Movie) item).getRunTime());
                ((MainActivity)getActivity()).setMoviePoster(((MovieBasicInfo) item).getPosterUrl());
                //((MainActivity)getActivity()).setMovieAgeRestriction(((Movie) item).getAgeRestriction());

            }
            //if (item instanceof Movie) {
              //  mBackgroundUri = ((Movie) item).getBackgroundImageUrl();
               // startBackgroundTimer();
            //}
        }
    }
}