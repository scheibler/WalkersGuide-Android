package org.walkersguide.android.ui.dialog;

import org.walkersguide.android.ui.view.builder.TextViewBuilder;
import android.widget.LinearLayout;
import org.walkersguide.android.BuildConfig;
import org.walkersguide.android.util.WalkersGuideService;
import org.walkersguide.android.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Intent;
import android.net.Uri;
import android.widget.Button;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import android.widget.TextView;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.ui.UiHelper;


public class ChangelogDialog extends DialogFragment {

    public static ChangelogDialog newInstance() {
        ChangelogDialog dialog = new ChangelogDialog();
        return dialog;
    }


    // dialog
    private static final String KEY_LIST_POSITION = "listPosition";

    private RecyclerView recyclerViewAttributes;
    private int listPosition;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        listPosition = savedInstanceState != null
            ? savedInstanceState.getInt(KEY_LIST_POSITION)
            : 0;
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        // custom view
        final ViewGroup nullParent = null;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_changelog, nullParent);

        recyclerViewAttributes = (RecyclerView) view.findViewById(R.id.recyclerViewChangelog);
        recyclerViewAttributes.setLayoutManager(new LinearLayoutManager(ChangelogDialog.this.getContext()));

        return new AlertDialog.Builder(getActivity())
            .setView(view)
            .setPositiveButton(
                    getResources().getString(R.string.dialogOK),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .setNeutralButton(
                    getResources().getString(R.string.buttonOpenAllChanges),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .create();
    }

    @Override public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog)getDialog();
        if(dialog != null) {

            // positive button
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    dismiss();
                }
            });

            // neutral button
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            buttonNeutral.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(
                            Uri.parse(
                                getResources().getString(R.string.variableChangeLogUrl)));
                    getActivity().startActivity(i);
                }
            });
        }

        recyclerViewAttributes.setAdapter(
                new ChangelogAdapter(ChangelogDialog.this.getContext()));

        recyclerViewAttributes.getLayoutManager().scrollToPosition(listPosition);
        recyclerViewAttributes.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerViewAttributes.getLayoutManager();
                if (layoutManager == null || newState != RecyclerView.SCROLL_STATE_IDLE) return;
                listPosition = layoutManager.findFirstVisibleItemPosition();
            }
            @Override public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(KEY_LIST_POSITION, listPosition);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            WalkersGuideService.startService();
        }
    }


    public static class ChangelogAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        public static class Item {
            public static final int TYPE_TEXT = 0;
            public static final int TYPE_HEADING = 1;

            public final int type;
            public final String text;

            public Item(int type, String text) {
                this.type = type;
                this.text = text;
            }
        }


        private Context context;
        private List<Item> itemList;

        public ChangelogAdapter(Context context) {
            this.context = context;
            this.itemList = new ArrayList<>();

            this.itemList.add(
                    new Item(
                        Item.TYPE_HEADING,
                        context.getResources().getString(R.string.labelInfoLastChangelog)));

            // version 3.2.3
            this.itemList.addAll(
                    createChangelogEntry(
                        context.getResources().getString(R.string.changesHeading3_2_3),
                        context.getResources().getStringArray(R.array.changesList3_2_3)));

            // version 3.2.2
            this.itemList.addAll(
                    createChangelogEntry(
                        context.getResources().getString(R.string.changesHeading3_2_2),
                        context.getResources().getStringArray(R.array.changesList3_2_2)));

            // version 3.2.1
            this.itemList.addAll(
                    createChangelogEntry(
                        context.getResources().getString(R.string.changesHeading3_2_1),
                        context.getResources().getStringArray(R.array.changesList3_2_1)));

            // version 3.2.0
            this.itemList.addAll(
                    createChangelogEntry(
                        context.getResources().getString(R.string.changesHeading3_2_0),
                        context.getResources().getStringArray(R.array.changesList3_2_0)));
        }

        private static List<Item> createChangelogEntry(String heading, String[] entries) {
            List<Item> changelogItemList = new ArrayList<>();

            // add heading
            changelogItemList.add(
                    new Item(Item.TYPE_HEADING, heading));

            // entries
            for (String line : entries) {
                changelogItemList.add(
                        new Item(Item.TYPE_TEXT, line));
            }

            return changelogItemList;
        }


        @Override public int getItemViewType(int position) {
            return this.itemList.get(position).type;
        }

        @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextViewBuilder textViewBuilder = new TextViewBuilder(this.context);
            if (viewType == Item.TYPE_HEADING) {
                textViewBuilder.isHeading();
                textViewBuilder.addTopMargin();
            }
            return new TextViewHolder(textViewBuilder.create());
        }

        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            int type = this.itemList.get(position).type;
            String text = this.itemList.get(position).text;

            TextView label = ((TextViewHolder) holder).label;
            if (type == Item.TYPE_HEADING) {
                label.setText(
                        UiHelper.boldAndRed(text));
            } else {
                label.setText(text);
            }
        }

        @Override public int getItemCount() {
            return this.itemList.size();
        }


        public static class TextViewHolder extends RecyclerView.ViewHolder {
            public TextView label;

            public TextViewHolder(TextView itemView) {
                super(itemView);
                this.label = itemView;
            }
        }
    }

}
