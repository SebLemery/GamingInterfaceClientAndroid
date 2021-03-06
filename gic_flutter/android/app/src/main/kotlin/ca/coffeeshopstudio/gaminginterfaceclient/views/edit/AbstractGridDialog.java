package ca.coffeeshopstudio.gaminginterfaceclient.views.edit;

import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.util.Objects;

import ca.coffeeshopstudio.gaminginterfaceclient.R;
import ca.coffeeshopstudio.gaminginterfaceclient.models.AbstractAdapter;

/**
 * Copyright [2019] [Terence Doerksen]
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
abstract class AbstractGridDialog extends AlertDialog {
    private int customCount = 0;
    private String imagePrefix;
    private int actionRequestCode;

    abstract void init();

    AbstractGridDialog(final Fragment fragment, final AbstractAdapter adapter) {
        super(Objects.requireNonNull(fragment.getActivity()));
        init();
        File file;
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            file = new File(getContext().getFilesDir(), imagePrefix + "_" + i + ".png");
            if (!file.exists()) {
                customCount = i;
                break;
            }
        }

        adapter.setCustomCount(customCount);
        setTitle(R.string.image_grid_title);

        GridView gridView = new GridView(fragment.getActivity());
        gridView.setAdapter(adapter);

        gridView.setNumColumns(2);               // Number of columns
        gridView.setChoiceMode(GridView.CHOICE_MODE_SINGLE);       // Choice mode
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < 2) { //import
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                        Toast.makeText(getContext(), R.string.android_too_old, Toast.LENGTH_SHORT).show();
                    } else {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("image/*");
                        fragment.startActivityForResult(intent, actionRequestCode);
                        dismiss();
                    }

                } else if (position <= customCount + 1) {
                    String path = fragment.getActivity().getFilesDir() + "/" + imagePrefix + "_" + (position - 2) + ".png";
                    ((GridDialogListener) fragment).onImageSelected(path, actionRequestCode);
                    dismiss();
                } else { // if (position - customCount <= adapter.getBuiltInResources().length + 1) {
                    ((GridDialogListener) fragment).onImageSelected(adapter.getBuiltInResources()[position - customCount - 2], actionRequestCode);
                    dismiss();
                }
            }
        });
        setView(gridView);
    }

    void setImagePrefix(String prefix) {
        imagePrefix = prefix;
    }

    void setActionRequestCode(int code) {
        actionRequestCode = code;
    }

    public interface GridDialogListener {
        void onImageSelected(String custom, int actionRequest);

        void onImageSelected(int builtIn, int actionRequest);
    }
}
