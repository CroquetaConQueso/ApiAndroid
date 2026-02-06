package com.example.trabajoapi;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trabajoapi.data.FichajeResponse;
import java.util.ArrayList;
import java.util.List;

public class FichajeAdapter extends RecyclerView.Adapter<FichajeAdapter.ViewHolder> {

    private List<FichajeResponse> lista = new ArrayList<>();

    public void setLista(List<FichajeResponse> nuevaLista) {
        this.lista = nuevaLista;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // CORRECCIÓN: Usamos R.layout.item_fichaje, NO el de Android por defecto
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fichaje, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FichajeResponse item = lista.get(position);

        String fecha = item.getFechaHora() != null ? item.getFechaHora().replace("T", " ") : "---";
        if (fecha.length() > 16) fecha = fecha.substring(0, 16);

        String tipo = item.getTipo() != null ? item.getTipo().toUpperCase() : "REGISTRO";

        holder.tvTipo.setText(tipo);
        holder.tvFecha.setText(fecha);
        // Formatear lat/lon a 4 decimales
        holder.tvUbi.setText(String.format("Lat: %.4f, Lon: %.4f", item.getLatitud(), item.getLongitud()));

        // Lógica visual
        if ("ENTRADA".equals(tipo)) {
            holder.tvTipo.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.pop_green));
            holder.imgIcon.setImageResource(R.drawable.ic_fichaje_entrada);
        } else {
            holder.tvTipo.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.pop_pink));
            holder.imgIcon.setImageResource(R.drawable.ic_fichaje_salida);
        }
    }

    @Override
    public int getItemCount() { return lista.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTipo, tvFecha, tvUbi;
        ImageView imgIcon;

        ViewHolder(View v) {
            super(v);
            // Estos IDs deben coincidir con tu item_fichaje.xml
            tvTipo = v.findViewById(R.id.tvTipoFichaje);
            tvFecha = v.findViewById(R.id.tvFechaFichaje);
            tvUbi = v.findViewById(R.id.tvUbicacionFichaje);
            imgIcon = v.findViewById(R.id.imgTipoFichaje);
        }
    }
}