package com.example.trabajoapi;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trabajoapi.data.TrabajadorResponse;
import java.util.List;

public class EmpleadoAdapter extends RecyclerView.Adapter<EmpleadoAdapter.ViewHolder> {

    private List<TrabajadorResponse> lista;

    public EmpleadoAdapter(List<TrabajadorResponse> lista) {
        this.lista = lista;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_empleado, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TrabajadorResponse item = lista.get(position);

        holder.tvNombre.setText(item.getNombre() + " " + item.getApellidos());
        holder.tvNif.setText(item.getNif());

        String rol = item.getRol() != null ? item.getRol() : "Trabajador";
        holder.tvRol.setText(rol.toUpperCase());

        // Cambiar color según rol
        if (rol.equalsIgnoreCase("Administrador") || rol.contains("Admin")) {
            holder.tvRol.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#000000"))); // Negro para jefes
        } else {
            holder.tvRol.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#00BCD4"))); // Cyan para currelas
        }
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvNif, tvRol;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreEmpleado);
            tvNif = itemView.findViewById(R.id.tvNifEmpleado);
            tvRol = itemView.findViewById(R.id.tvRolEmpleado);
        }
    }
}