package com.example.chatapp.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.example.chatapp.adapter.GroupChatAdapter;
import com.example.chatapp.model.GroupChat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class GroupChatFragment extends Fragment {

    private RecyclerView recyclerView;

    private FirebaseAuth firebaseAuth;

    private List<GroupChat> groupChatList;

    private GroupChatAdapter groupChatAdapter;

    private EditText search_group;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_group_chat, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        firebaseAuth = FirebaseAuth.getInstance();

        bindingView(view);

        // search group
        search_group.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                searchGroupChatList(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        loadGroupChatList();

        return view;
    }

    private void bindingView(View view) {
        search_group = view.findViewById(R.id.search_group);
    }

    private void loadGroupChatList() {
        groupChatList = new ArrayList<>();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                groupChatList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    // if logging user exist in group chat, display group chat
                    if (dataSnapshot.child("Participants").child(firebaseAuth.getUid()).exists()) {
                        GroupChat groupChat = dataSnapshot.getValue(GroupChat.class);
                        groupChatList.add(groupChat);
                    }
                }

                groupChatAdapter = new GroupChatAdapter(getContext(), groupChatList);
                recyclerView.setAdapter(groupChatAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void searchGroupChatList(String text) {
        groupChatList = new ArrayList<>();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                groupChatList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    // if logging user exist in group chat, display group chat
                    if (dataSnapshot.child("Participants").child(firebaseAuth.getUid()).exists()) {
                        // search group by group name
                        if (dataSnapshot.child("groupTitle").toString().toLowerCase().contains(text.toLowerCase())) {
                            GroupChat groupChat = dataSnapshot.getValue(GroupChat.class);
                            groupChatList.add(groupChat);
                        }
                    }
                }

                groupChatAdapter = new GroupChatAdapter(getContext(), groupChatList);
                recyclerView.setAdapter(groupChatAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}