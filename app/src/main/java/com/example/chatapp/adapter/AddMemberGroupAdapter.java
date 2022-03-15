package com.example.chatapp.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.R;
import com.example.chatapp.model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class AddMemberGroupAdapter extends RecyclerView.Adapter<AddMemberGroupAdapter.ViewHolder> {

    public static final String ROLE_CREATOR = "creator";
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_MEMBER = "member";
    public static final String GROUPS_NODE = "Groups";
    public static final String PARTICIPANTS_NODE = "Participants";

    private Context context;
    private List<User> userList;
    private String groupId, myGroupRole; //creator, admin, member


    public AddMemberGroupAdapter(Context context, List<User> userList, String groupId, String myGroupRole) {
        this.context = context;
        this.userList = userList;
        this.groupId = groupId;
        this.myGroupRole = myGroupRole;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User model = userList.get(position);

        loadInfoUser(holder, model);

        // check if user already added to the group chat
        checkIfUserAlreadyInGroupChat(holder, model);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // check if user already added to the group
                // if added: show option remove, set role admin, remove role admin
                // if not added, show add member option
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference(GROUPS_NODE);
                reference.child(groupId).child(PARTICIPANTS_NODE).child(model.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // if user exist in group
                            String hisPreviousRole = snapshot.child("role").getValue().toString();

                            String options[];

                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setTitle("Choose option");
                            if (myGroupRole.equals(ROLE_CREATOR)) {
                                if (hisPreviousRole.equals(ROLE_ADMIN)) {
                                    // if logging user is creator group, his role is admin
                                    options = new String[]{"Remove Admin", "Remove User"};
                                    builder.setItems(options, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if (i == 0) {
                                                // Remove Admin clicked
                                                removeRoleAdmin(model);
                                            } else {
                                                // Remove User clicked
                                                removeUserFromGroup(model);
                                            }
                                        }
                                    }).show();
                                } else if (hisPreviousRole.equals(ROLE_MEMBER)) {
                                    // if logged user is creator, choosing user is member
                                    options = new String[]{"Set Admin", "Remove User"};
                                    builder.setItems(options, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if (i == 0) {
                                                // set role Admin clicked
                                                setRoleAdmin(model);
                                            } else {
                                                // Remove User clicked
                                                removeUserFromGroup(model);
                                            }
                                        }
                                    }).show();
                                }
                            } else if (myGroupRole.equals(ROLE_ADMIN)) {
                                if (hisPreviousRole.equals(ROLE_CREATOR)) {
                                    Toast.makeText(context, "Creator of group", Toast.LENGTH_SHORT).show();
                                } else if (hisPreviousRole.equals("admin")) {
                                    // im admin, he is admin
                                    options = new String[]{"Remove Admin", "Remove User"};
                                    builder.setItems(options, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if (i == 0) {
                                                // Remove Admin clicked
                                                removeRoleAdmin(model);
                                            } else {
                                                // Remove User clicked
                                                removeUserFromGroup(model);
                                            }
                                        }
                                    }).show();
                                } else if (hisPreviousRole.equals(ROLE_MEMBER)) {
                                    // im admin, he is member
                                    options = new String[]{"Set Admin", "Remove User"};
                                    builder.setItems(options, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if (i == 0) {
                                                // set role Admin clicked
                                                setRoleAdmin(model);
                                            } else {
                                                // Remove User clicked
                                                removeUserFromGroup(model);
                                            }
                                        }
                                    }).show();
                                }
                            }
                        } else {
                            // if user not in group yet
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setTitle("Add Member").setMessage("Add user to the group?")
                                    .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            // add user to the group
                                            addMemberToTheGroup(model);
                                        }
                                    })
                                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            dialogInterface.dismiss();
                                        }
                                    }).show();

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });
    }

    private void addMemberToTheGroup(User model) {
        String time = String.valueOf(System.currentTimeMillis());
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("uid", model.getId());
        hashMap.put("role", ROLE_MEMBER);
        hashMap.put("timestamp", time);

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.child(groupId).child(PARTICIPANTS_NODE).child(model.getId()).setValue(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(context, "Add user success", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(context, "Fail: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setRoleAdmin(User model) {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("role", ROLE_ADMIN);

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.child(groupId).child(PARTICIPANTS_NODE).child(model.getId()).updateChildren(hashMap).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(context, "The user is now admin", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(context, "Fail: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeUserFromGroup(User model) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.child(groupId).child(PARTICIPANTS_NODE).child(model.getId()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(context, "User removed from group", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(context, "Fail: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeRoleAdmin(User model) {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("role", ROLE_MEMBER);

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.child(groupId).child(PARTICIPANTS_NODE).child(model.getId()).updateChildren(hashMap).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(context, "The user is now member", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(context, "Fail: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkIfUserAlreadyInGroupChat(@NonNull ViewHolder holder, User model) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference(GROUPS_NODE);
        reference.child(groupId).child(PARTICIPANTS_NODE).child(model.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // member already added to the group
                    holder.role_group.setVisibility(View.VISIBLE);
                    holder.role_group.setText("(" + snapshot.child("role").getValue().toString() + ")");
                } else {
                    // member not in group
                    holder.role_group.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadInfoUser(@NonNull ViewHolder holder, User model) {
        if (model.getImageURL().equals("default")) {
            holder.profile_image.setImageResource(R.mipmap.ic_launcher);
        } else {
            Glide.with(context).load(model.getImageURL()).into(holder.profile_image);
        }
        holder.username.setText(model.getUsername());
        holder.email.setText(model.getEmail());
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private CircleImageView profile_image;
        private TextView username, email, role_group;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            profile_image = itemView.findViewById(R.id.profile_image);
            username = itemView.findViewById(R.id.group_sender);
            email = itemView.findViewById(R.id.group_message);
            role_group = itemView.findViewById(R.id.role_group);
        }
    }
}
