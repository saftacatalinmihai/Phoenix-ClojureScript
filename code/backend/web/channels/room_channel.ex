defmodule Backend.RoomChannel do
    use Phoenix.Channel

    def join("room:lobby", _message, socket) do
      {:ok, socket}
    end

    def handle_in("new_actor", %{"name" => name}, socket) do
        IO.puts "new actor received"

        case File.read("code/template_actor.ex") do
          {:ok, body} ->
            new_actor_module = String.replace(body, "{{actor_name}}", name)
            {:ok, file} = File.open "code/" <> name <> ".ex", [:write]
            IO.binwrite file, new_actor_module
          {:error, reason} -> IO.inspect reason
        end
        IO.inspect Code.eval_file("code/" <> name <> ".ex")
        pid = Code.eval_string(name <> ".start_link()")
        IO.inspect pid

        {:reply, {:ok, %{:name => name}}, socket}
    end
end