package uni.tesis.interfazfinal;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.List;

public class GrabacionAdapter extends ArrayAdapter<GrabacionModel> {
    public GrabacionAdapter(Context context, List<GrabacionModel> grabaciones) {
        super(context, 0, grabaciones);
    }
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Inflar el diseño de cada elemento de la lista
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_with_button, parent, false);

        }

        // Obtener el modelo de grabación actual
        GrabacionModel grabacion = getItem(position);

        // Asignar el nombre de la grabación al elemento de la lista
        TextView textViewNombre = convertView.findViewById(R.id.textViewNombre);
        textViewNombre.setText(grabacion != null ? grabacion.getNombre() : "");

        // Agregar un botón de eliminación
        Button btnEliminar = convertView.findViewById(R.id.btnEliminar);
        btnEliminar.setOnClickListener(v -> {
            // Obtener el nombre de la grabación asociada a este elemento
            String nombreGrabacion = grabacion != null ? grabacion.getNombre() : "";

            // Llamar a la función para eliminar la grabación
            eliminarGrabacion(nombreGrabacion);
        });
        return convertView;
    }

    private void eliminarGrabacion(String nombreGrabacion) {
        // ... Lógica para eliminar la grabación del sistema de archivos
        File archivo = new File(getContext().getExternalCacheDir(), nombreGrabacion);
        if (archivo.exists()) {
            if (archivo.delete()) {
                // El archivo se eliminó correctamente
                // Puedes notificar al adaptador que los datos han cambiado y deben actualizarse en la lista
                notifyDataSetChanged();
            } else {
                // No se pudo eliminar el archivo, manejar según sea necesario
            }
        }
    }

}

