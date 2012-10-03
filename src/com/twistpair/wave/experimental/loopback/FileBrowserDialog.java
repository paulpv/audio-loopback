package com.twistpair.wave.experimental.loopback;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

//
// Ideas:
//  https://github.com/mburman/Android-File-Explore/blob/master/FileExplore/src/com/mburman/fileexplore/FileExplore.java
//  https://github.com/vaal12/AndroidFileBrowser
//
public class FileBrowserDialog extends DialogFragment
{
    private static final String TAG = FileBrowserDialog.class.getSimpleName();

    public interface FileBrowserDialogListener
    {
        void onFinishFileSelected(String path);
    }
	
    public FileBrowserDialog()
    {
        // Empty constructor required for DialogFragment
    }

    public static FileBrowserDialog newInstance(Context context, String path, String filterFileNameEndsWith)
    {
        Bundle args = new Bundle();
        args.putString("path", path);
        args.putString("filterFileNameEndsWith", filterFileNameEndsWith);

        FileBrowserDialog fileBrowserDialog = new FileBrowserDialog();
        fileBrowserDialog.setArguments(args);
        return fileBrowserDialog;
    }

    public static void showDialog(FragmentActivity activity, String path, String filterFileNameEndsWith)
    {
        FileBrowserDialog fileBrowserDialog = newInstance(activity, path, filterFileNameEndsWith);
        FragmentManager fm = activity.getSupportFragmentManager();
        fileBrowserDialog.show(fm, "fragment_file_browser");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        final FragmentActivity activity = getActivity();
        Bundle args = getArguments();

        String path = args.getString("path");
        final String filterFileNameEndsWith = args.getString("filterFileNameEndsWith");

        if (path == null || path.length() == 0)
        {
            path = Environment.getExternalStorageDirectory().getAbsolutePath();
        }

        // If path points to a file, get its directory
        File temp = new File(path);
        if (temp.isFile() && temp.getParent() != null)
        {
            temp = temp.getParentFile();
        }

        final File root = temp;

        AlertDialog.Builder builder = new Builder(activity);

        // TODO:(pv) Show progress dialog here
		// This may need to be done as follows:
		//   1) Start an async task here
		//   2) Return the progress dialog
		//   3) After the async task ends, remove progress dialog and create browser dialog
        final ArrayList<FileBrowserItem> fileList = getDirectoryItems(root, filterFileNameEndsWith);
        if (fileList == null || fileList.size() == 0)
        {
            Log.e(TAG, "No files loaded");
            return builder.create();
        }

        ArrayAdapter<FileBrowserItem> adapter =
            new ArrayAdapter<FileBrowserItem>(activity, android.R.layout.select_dialog_item, android.R.id.text1, fileList)
            {
                @Override
                public View getView(int position, View convertView, ViewGroup parent)
                {
                    // creates view
                    View view = super.getView(position, convertView, parent);
                    TextView textView = (TextView) view.findViewById(android.R.id.text1);

                    // put the image on the text view
                    textView.setCompoundDrawablesWithIntrinsicBounds(fileList.get(position).icon, 0, 0, 0);

                    // add margin between image and text (support various screen densities)
                    int dp5 = (int) (5 * view.getResources().getDisplayMetrics().density + 0.5f);
                    textView.setCompoundDrawablePadding(dp5);

                    return view;
                }
            };

        // Sorts the list with "Up" above files above directories.
        adapter.sort(new Comparator<FileBrowserItem>()
        {
            public int compare(FileBrowserItem object1, FileBrowserItem object2)
            {
                if (object1.icon == R.drawable.directory_up)
                {
                    return -1;
                }

                if (object2.icon == R.drawable.directory_up)
                {
                    return 1;
                }

                if (object1.icon == object2.icon)
                {
                    return object1.name.compareTo(object2.name);
                }

                return (object1.icon == R.drawable.directory_icon) ? 1 : -1;
            };
        });

        String title;
        if (filterFileNameEndsWith == null || filterFileNameEndsWith.length() == 0)
        {
            title = "Choose a file";
        }
        else
        {
            title = "Choose a " + filterFileNameEndsWith + " file";
        }
        builder.setTitle(title);

        builder.setAdapter(adapter, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                FileBrowserItem chosenFileItem = fileList.get(which);
                switch (chosenFileItem.icon)
                {
                    case R.drawable.directory_up:
                    {
                        // Browse up to the parent directory
                        File chosenPath = new File(root.getAbsolutePath()).getParentFile();

                        getDialog().dismiss();

                        showDialog(activity, chosenPath.getAbsolutePath(), filterFileNameEndsWith);
                        break;
                    }
                    case R.drawable.directory_icon:
                    {
                        // Browse down to the child directory
                        File chosenPath = new File(root.getAbsolutePath(), chosenFileItem.name);

                        getDialog().dismiss();

                        showDialog(activity, chosenPath.getAbsolutePath(), filterFileNameEndsWith);
                        break;
                    }
                    case R.drawable.file_icon:
                    {
                        // File selected; perform action
                        File chosenPath = new File(root.getAbsolutePath(), chosenFileItem.name);

                        FileBrowserDialogListener listener = (FileBrowserDialogListener) activity;
                        listener.onFinishFileSelected(chosenPath.getAbsolutePath());

                        getDialog().dismiss();
                        break;
                    }
                }
            }
        });

        return builder.create();
    }

    private static class FileBrowserItem
    {
        public String name;
        public int    icon;

        public FileBrowserItem(String file, Integer icon)
        {
            this.name = file;
            this.icon = icon;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }

    private static ArrayList<FileBrowserItem> getDirectoryItems(File root, String filterFileNameEndsWith)
    {
        ArrayList<FileBrowserItem> fileList = new ArrayList<FileBrowserItem>();

        if (!root.exists())
        {
            Log.e(TAG, "path does not exist");
            return fileList;
        }

        if (filterFileNameEndsWith == null)
        {
            filterFileNameEndsWith = "";
        }

        filterFileNameEndsWith = filterFileNameEndsWith.toLowerCase();

        String[] names = root.list();
        for (String name : names)
        {
            File file = new File(root, name);

            if (file.isHidden())
            {
                continue;
            }

            FileBrowserItem fileItem = null;

            if (file.isDirectory())
            {
                fileItem = new FileBrowserItem(name, R.drawable.directory_icon);
                Log.d("DIRECTORY", fileItem.name);
            }
            else if (file.isFile())
            {
                if (filterFileNameEndsWith.length() == 0 || name.toLowerCase().endsWith(filterFileNameEndsWith))
                {
                    fileItem = new FileBrowserItem(name, R.drawable.file_icon);
                    Log.d("FILE", fileItem.name);
                }
            }

            if (fileItem != null)
            {
                fileList.add(fileItem);
            }
        }

        String parent = root.getParent();
        if (!parent.equals("/mnt"))
        {
            FileBrowserItem up = new FileBrowserItem("Up", R.drawable.directory_up);
            fileList.add(0, up);
        }

        return fileList;
    }

    /*
    public static Dialog createFileBrowserDialog(final Activity activity, Bundle args)
    {
        String path = args.getString("path");
        final String filterFileNameEndsWith = args.getString("filterFileNameEndsWith");

        if (path == null || path.length() == 0)
        {
            path = Environment.getExternalStorageDirectory().getAbsolutePath();
        }

        // If path points to a file, get its directory
        File temp = new File(path);
        if (temp.isFile() && temp.getParent() != null)
        {
            temp = temp.getParentFile();
        }

        final File root = temp;

        AlertDialog.Builder builder = new Builder(activity);

        final ArrayList<FileBrowserItem> fileList = getDirectoryItems(root, filterFileNameEndsWith);
        if (fileList == null || fileList.size() == 0)
        {
            Log.e(TAG, "No files loaded");
            return builder.create();
        }

        ArrayAdapter<FileBrowserItem> adapter =
            new ArrayAdapter<FileBrowserItem>(activity, android.R.layout.select_dialog_item, android.R.id.text1, fileList)
            {
                @Override
                public View getView(int position, View convertView, ViewGroup parent)
                {
                    // creates view
                    View view = super.getView(position, convertView, parent);
                    TextView textView = (TextView) view.findViewById(android.R.id.text1);

                    // put the image on the text view
                    textView.setCompoundDrawablesWithIntrinsicBounds(fileList.get(position).icon, 0, 0, 0);

                    // add margin between image and text (support various screen densities)
                    int dp5 = (int) (5 * view.getResources().getDisplayMetrics().density + 0.5f);
                    textView.setCompoundDrawablePadding(dp5);

                    return view;
                }
            };

        // Sorts the list with "Up" above files above directories.
        adapter.sort(new Comparator<FileBrowserItem>()
        {
            public int compare(FileBrowserItem object1, FileBrowserItem object2)
            {
                if (object1.icon == R.drawable.directory_up)
                {
                    return -1;
                }

                if (object2.icon == R.drawable.directory_up)
                {
                    return 1;
                }

                if (object1.icon == object2.icon)
                {
                    return object1.name.compareTo(object2.name);
                }

                return (object1.icon == R.drawable.directory_icon) ? 1 : -1;
            };
        });

        String title;
        if (filterFileNameEndsWith == null || filterFileNameEndsWith.length() == 0)
        {
            title = "Choose a file";
        }
        else
        {
            title = "Choose a " + filterFileNameEndsWith + " file";
        }
        builder.setTitle(title);

        builder.setAdapter(adapter, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                FileBrowserItem chosenFileItem = fileList.get(which);
                switch (chosenFileItem.icon)
                {
                    case R.drawable.directory_up:
                    {
                        // Browse up to the parent directory
                        File chosenPath = new File(root.getAbsolutePath()).getParentFile();

                        // TODO:(pv) Make Fragment friendly
                        activity.removeDialog(dialogId);

                        showDialog(activity, dialogId, chosenPath.getAbsolutePath(), filterFileNameEndsWith);
                        break;
                    }
                    case R.drawable.directory_icon:
                    {
                        // Browse down to the child directory
                        File chosenPath = new File(root.getAbsolutePath(), chosenFileItem.name);

                        // TODO:(pv) Make Fragment friendly
                        activity.removeDialog(dialogId);

                        showDialog(activity, dialogId, chosenPath.getAbsolutePath(), filterFileNameEndsWith);
                        break;
                    }
                    case R.drawable.file_icon:
                    {
                        // File selected; perform action
                        File chosenPath = new File(root.getAbsolutePath(), chosenFileItem.name);

                        // TODO:(pv) Pass result back out to a listener                     
                        TextView textViewSourceFile = (TextView) findViewById(R.id.textViewSourceFile);
                        textViewSourceFile.setText(chosenPath.getAbsolutePath());
                        break;
                    }
                }
            }
        });
        return builder.create();
    }
    */
}
