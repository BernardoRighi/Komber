package komber.helper;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import komber.activity.PassageiroActivity;
import komber.activity.RequisicoesActivity;
import komber.config.ConfiguracaoFirebase;
import komber.model.Usuario;

public class UsuarioFirebase {

    public static FirebaseUser getUsuarioatual(){
        FirebaseAuth usuario = ConfiguracaoFirebase.getFirebaseAutenticacao();
        return usuario.getCurrentUser();
    }

    public static Usuario getDadosUsuarioLogado(){

        FirebaseUser firebaseUser = getUsuarioatual();

        final Usuario usuario = new Usuario();
        usuario.setId(firebaseUser.getUid());
        usuario.setEmail(firebaseUser.getEmail());
        usuario.setNome(firebaseUser.getDisplayName());

        DatabaseReference firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();

        DatabaseReference usuario1 = firebaseRef.child("usuarios");

        Query requisicoesPesquisa = usuario1.orderByChild("id")
                .equalTo(usuario.getId());

        requisicoesPesquisa.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    Usuario usuario2 = ds.getValue(Usuario.class);

                    if(usuario2.getId().equals(usuario.getId())){
                        usuario.setNome(usuario2.getNome());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        return usuario;
    }

    public static boolean atualizarNomeUsuario(String nome){

        try{
            FirebaseUser user = getUsuarioatual();
            UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                    .setDisplayName( nome )
                    .build();
            user.updateProfile( profile ).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (!task.isSuccessful()){
                        Log.d("Perfil","Erro ao atualizar nome de perfil");
                    }
                }
            });

            return true;

        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public static void redirecionaUsuarioLogado(final Activity activity){

        final FirebaseUser user = getUsuarioatual();

        // Redirecionar o usuario apenas se ele estiver logado.
        // Se não estiver logado não fazer o redirecionamento
        if (user != null){

            DatabaseReference usuariosRef = ConfiguracaoFirebase.getFirebaseDatabase()
                    .child("usuarios")
                    .child(getIdentificadorUsuario());

            usuariosRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    // Recuperar o objeto usuario
                    Usuario usuario = dataSnapshot.getValue( Usuario.class);

                    String tipoUsuario = usuario.getTipo();
                    if (tipoUsuario.equals("M")){
                        // Envia para a tela de requisições
                        activity.startActivity(new Intent(activity, RequisicoesActivity.class));

                    }else{
                        // Envia para a tela do maps
                        activity.startActivity(new Intent(activity, PassageiroActivity.class));

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    public static void atualizarDadosLocalizacao(double lat, double lon){

        DatabaseReference localUsuario = ConfiguracaoFirebase.getFirebaseDatabase()
                .child("local_usuario");

        GeoFire geoFire = new GeoFire(localUsuario);

        Usuario usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();

        geoFire.setLocation(
                usuarioLogado.getId(),
                new GeoLocation(lat, lon),
                new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if (error != null){
                            Log.d("Erro", "Erro ao salvar local!");
                        }
                    }
                });

    }

    public static String getIdentificadorUsuario(){
        return getUsuarioatual().getUid();
    }

}
