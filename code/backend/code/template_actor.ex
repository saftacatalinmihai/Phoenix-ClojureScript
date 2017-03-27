defmodule {{actor_name}} do
    use GenServer

    def start_link do
       GenServer.start_link(__MODULE__, :ok, [])
      end

    def init(:ok) do
        {:ok, %{}}
    end

    def handle_call("ping", _from, state) do
        {:reply, "pong", state}
    end

    def handle_cast("ping", state) do
      {:noreply, state}
    end
end