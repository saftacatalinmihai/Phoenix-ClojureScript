defmodule Backend.RoomChannel do
    use Phoenix.Channel

    def join("room:lobby", _message, socket) do
      {:ok, socket}
    end

    def get_module(module_string) do
      :erlang.binary_to_existing_atom(module_string <> <<>>, :utf8)
    end

    def handle_in("get_actors", attrs, socket) do
        actor_modules = Path.wildcard("code/*.ex") |>
            Enum.map(fn file ->
                String.replace_prefix(file, "code/", "") |>
                String.replace_suffix(".ex", "")
            end)

        {:reply, {:ok, %{:actors => actor_modules}}, socket}
    end

    def handle_in("new_actor", %{"name" => name}, socket) do
        IO.puts "new actor received"

        case File.read("code/template/actor.ex") do
          {:ok, body} ->
            new_actor_module = String.replace(body, "{{actor_name}}", name)
            {:ok, file} = File.open "code/#{name}.ex", [:write]
            IO.binwrite file, new_actor_module
          {:error, reason} -> IO.inspect reason
        end
        Code.eval_file("code/#{name}.ex")
        {:ok, pid} = :"Elixir.#{name}".start_link
        IO.inspect pid

        {:reply, {:ok, %{:name => name, :pid => to_string(:erlang.pid_to_list(pid))}}, socket}
    end

    def handle_in("send_msg", %{"name" => name, "to_pid" => pid, "msg" => msg}, socket) do
        IO.puts "send msg"
        pid = :erlang.list_to_pid(to_charlist(pid))
        rec = :"Elixir.#{name}".send_msg(pid, msg)
        {:reply, {:ok, %{:received => rec}}, socket}
    end

    def handle_in("update_actor", %{"name" => name, "actor_code" => code }, socket) do
      IO.puts "update actor received"

      {:ok, file} = File.open "code/#{name}.ex", [:write]
      IO.binwrite file, code
      IO.inspect Code.eval_file("code/#{name}.ex")

      {:reply, {:ok, %{:name => name}}, socket}
    end
end