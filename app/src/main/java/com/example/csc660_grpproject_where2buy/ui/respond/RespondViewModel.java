package com.example.csc660_grpproject_where2buy.ui.respond;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class RespondViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public RespondViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is notifications fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}